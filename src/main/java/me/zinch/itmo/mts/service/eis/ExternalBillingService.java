package me.zinch.itmo.mts.service.eis;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.resource.ResourceException;
import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.domain.entity.Order;
import me.zinch.itmo.mts.domain.entity.OrderEisProvisioning;
import me.zinch.itmo.mts.domain.entity.OrderEisSubscription;
import me.zinch.itmo.mts.domain.entity.OrderItem;
import me.zinch.itmo.mts.domain.enums.EisProvisioningStatus;
import me.zinch.itmo.mts.domain.enums.OrderStatus;
import me.zinch.itmo.mts.eis.BillingEisConnection;
import me.zinch.itmo.mts.eis.BillingEisConnectionFactory;
import me.zinch.itmo.mts.eis.ProvisionResult;
import me.zinch.itmo.mts.eis.ProvisionServicesRequest;
import me.zinch.itmo.mts.repository.OrderEisProvisioningRepository;
import me.zinch.itmo.mts.repository.OrderEisSubscriptionRepository;
import me.zinch.itmo.mts.repository.OrderRepository;
import me.zinch.itmo.mts.service.ServiceException;

@Service
@RequiredArgsConstructor
public class ExternalBillingService {

    private final BillingEisConnectionFactory billingEisConnectionFactory;
    private final OrderRepository orderRepository;
    private final OrderEisProvisioningRepository provisioningRepository;
    private final OrderEisSubscriptionRepository subscriptionRepository;
    private final TransactionTemplate jtaTransactionTemplate;

    public Order provisionPaidOrder(UUID orderId) {
        ProvisionServicesRequest request = jtaTransactionTemplate.execute(status -> prepareProvisioning(orderId));
        if (request == null) {
            return orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
        }

        ProvisionResult result;
        try (BillingEisConnection connection = billingEisConnectionFactory.getConnection()) {
            result = connection.provisionServices(request);
        } catch (ResourceException exception) {
            markFailed(orderId, exception.getMessage());
            throw new ServiceException("Не удалось активировать услуги в Kill Bill: " + exception.getMessage());
        }

        if (!result.success()) {
            markFailed(orderId, result.errorMessage());
            throw new ServiceException("Kill Bill не активировал услуги: " + result.errorMessage());
        }
        return markActivated(orderId, result);
    }

    private ProvisionServicesRequest prepareProvisioning(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
        if (order.getStatus() == OrderStatus.SERVICES_ACTIVATED) {
            return null;
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ServiceException("Передать в EIS можно только заказ в статусе PAID");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OrderEisProvisioning provisioning = provisioningRepository.findById(orderId)
                .orElseGet(() -> OrderEisProvisioning.builder()
                        .orderId(orderId)
                        .createdAt(now)
                        .build());
        provisioning.setStatus(EisProvisioningStatus.PROCESSING);
        provisioning.setErrorMessage(null);
        provisioning.setUpdatedAt(now);
        provisioningRepository.save(provisioning);

        List<ProvisionServicesRequest.ServiceData> services = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            String code = item.getProduct().getServiceCode();
            if (code == null || code.isBlank()) {
                throw new ServiceException("Для продукта не задан код услуги: " + item.getProduct().getName());
            }
            for (int index = 0; index < item.getQuantity(); index++) {
                services.add(new ProvisionServicesRequest.ServiceData(code, item.getProduct().getName()));
            }
        }
        return new ProvisionServicesRequest(orderId.toString(),
                new ProvisionServicesRequest.CustomerData("customer-" + order.getCustomer().getId(),
                        order.getCustomer().getName(), order.getCustomer().getEmail()),
                services);
    }

    private Order markActivated(UUID orderId, ProvisionResult result) {
        return jtaTransactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
            OrderEisProvisioning provisioning = provisioningRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Не найден журнал интеграции EIS для заказа: " + orderId));
            subscriptionRepository.deleteAllByOrderId(orderId);
            result.subscriptions().forEach(subscription -> subscriptionRepository.save(OrderEisSubscription.builder()
                    .orderId(orderId)
                    .serviceCode(subscription.serviceCode())
                    .externalSubscriptionId(subscription.externalSubscriptionId())
                    .build()));
            provisioning.setStatus(EisProvisioningStatus.ACTIVATED);
            provisioning.setExternalCustomerId(result.externalCustomerId());
            provisioning.setErrorMessage(null);
            provisioning.setUpdatedAt(OffsetDateTime.now());
            order.setStatus(OrderStatus.SERVICES_ACTIVATED);
            order.setUpdatedAt(OffsetDateTime.now());
            return orderRepository.saveAndFlush(order);
        });
    }

    private void markFailed(UUID orderId, String errorMessage) {
        jtaTransactionTemplate.executeWithoutResult(status -> provisioningRepository.findById(orderId)
                .ifPresent(provisioning -> {
                    provisioning.setStatus(EisProvisioningStatus.FAILED);
                    provisioning.setErrorMessage(errorMessage);
                    provisioning.setUpdatedAt(OffsetDateTime.now());
                }));
    }
}
