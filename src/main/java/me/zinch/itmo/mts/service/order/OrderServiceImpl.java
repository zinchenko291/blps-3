package me.zinch.itmo.mts.service.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zinch.itmo.mts.config.YooKassaProperties;
import me.zinch.itmo.mts.domain.entity.Customer;
import me.zinch.itmo.mts.domain.entity.Order;
import me.zinch.itmo.mts.domain.entity.OrderItem;
import me.zinch.itmo.mts.domain.entity.Product;
import me.zinch.itmo.mts.domain.entity.User;
import me.zinch.itmo.mts.domain.enums.OrderStatus;
import me.zinch.itmo.mts.domain.enums.UserRole;
import me.zinch.itmo.mts.domain.enums.YooKassaPaymentStatus;
import me.zinch.itmo.mts.domain.payment.Payment;
import me.zinch.itmo.mts.repository.CustomerRepository;
import me.zinch.itmo.mts.repository.OrderRepository;
import me.zinch.itmo.mts.repository.ProductRepository;
import me.zinch.itmo.mts.repository.UserRepository;
import me.zinch.itmo.mts.repository.payment.PaymentRepository;
import me.zinch.itmo.mts.service.ServiceException;
import me.zinch.itmo.mts.service.order.dto.CreateOrderRequest;
import me.zinch.itmo.mts.service.order.dto.OrderItemRequest;
import me.zinch.itmo.mts.service.order.message.KafkaTopics;
import me.zinch.itmo.mts.service.order.message.event.OrderPlaced;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkCreated;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkRequested;
import me.zinch.itmo.mts.service.order.message.producer.KafkaMessageProducer;
import me.zinch.itmo.mts.service.payment.CreatePaymentResult;
import me.zinch.itmo.mts.service.payment.YooKassaPaymentService;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final YooKassaPaymentService yooKassaPaymentService;
    private final YooKassaProperties yooKassaProperties;
    private final TransactionTemplate jtaTransactionTemplate;

    @Override
    public Order createOrder(@Valid CreateOrderRequest request) {
        Order savedOrder = jtaTransactionTemplate.execute(status -> {
            log.info("Creating order for customer email={}", request.email());

            Customer customer = upsertCustomer(request.customerName(), request.phoneNumber(), request.email());
            Order order = new Order();
            order.setCustomer(customer);
            order.setStatus(OrderStatus.NEW);
            order.setCreatedAt(OffsetDateTime.now());
            order.setUpdatedAt(OffsetDateTime.now());
            replaceItems(order, request.items());
            return orderRepository.save(order);
        });
        if (savedOrder == null) {
            throw new ServiceException("Не удалось создать заказ");
        }
        log.info("Order created: orderId={}, customerId={}, items={}",
                savedOrder.getId(), savedOrder.getCustomer().getId(), savedOrder.getItems().size());
        return savedOrder;
    }

    @Override
    @PreAuthorize("hasAuthority('ORDER_READ')")
    public Order getOrderForUser(UUID userId, UUID orderId) {
        return jtaTransactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ServiceException("Пользователь не найден: " + userId));
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
            if (user.getRole() == UserRole.SENIOR_MANAGER)
                return order;
            if (user.getRole() == UserRole.MANAGER) {
                assertManagedBy(order, userId);
                return order;
            }
            throw new ServiceException("Пользователь должен иметь роль MANAGER или SENIOR_MANAGER");
        });
    }

    @Override
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public Order updateOrder(UUID orderId, UUID managerId, @Valid CreateOrderRequest request) {
        return jtaTransactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));

            assertManagedBy(order, managerId);
            assertStatusIsNew(order);

            Customer customer = upsertCustomer(request.customerName(), request.phoneNumber(), request.email());
            order.setCustomer(customer);
            replaceItems(order, request.items());
            order.setUpdatedAt(OffsetDateTime.now());

            return orderRepository.save(order);
        });
    }

    @Override
    @PreAuthorize("hasAuthority('ORDER_DELETE')")
    public void deleteOrder(UUID orderId) {
        log.info("Deleting order: orderId={}", orderId);
        jtaTransactionTemplate.executeWithoutResult(status -> {
            if (paymentRepository.findByOrderId(orderId).isPresent()) {
                throw new ServiceException("Нельзя удалить заказ со созданной оплатой: " + orderId);
            }
            orderRepository.delete(orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId)));
        });
    }

    @Override
    @PreAuthorize("hasAuthority('ORDER_LIST_OWN') or hasAuthority('ORDER_LIST_ALL')")
    public Page<Order> getOrdersForUser(UUID userId, Pageable pageable) {
        return jtaTransactionTemplate.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ServiceException("Пользователь не найден: " + userId));
            if (user.getRole() == UserRole.SENIOR_MANAGER)
                return orderRepository.findAll(pageable);
            if (user.getRole() == UserRole.MANAGER)
                return orderRepository.findAllByManagerId(userId, pageable);
            throw new ServiceException("Пользователь должен иметь роль MANAGER или SENIOR_MANAGER");
        });
    }

    @Override
    @PreAuthorize("hasAuthority('ORDER_ASSIGN_MANAGER')")
    public Order assignManager(UUID orderId, UUID managerId) {
        Order savedOrder = jtaTransactionTemplate.execute(status -> {
            log.info("Assigning manager to order: orderId={}, managerId={}", orderId, managerId);
            User manager = requireRole(managerId, UserRole.MANAGER);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
            order.setManager(manager);
            order.setUpdatedAt(OffsetDateTime.now());
            return orderRepository.save(order);
        });
        if (savedOrder == null) {
            throw new ServiceException("Не удалось назначить менеджера для заказа: " + orderId);
        }
        log.info("Manager assigned to order: orderId={}, managerId={}", savedOrder.getId(), managerId);
        return savedOrder;
    }

    @Override
    @PreAuthorize("hasAuthority('ORDER_CHANGE_STATUS')")
    public Order changeStatus(UUID orderId, UUID managerId, OrderStatus newStatus) {
        log.info("Changing order status: orderId={}, managerId={}, newStatus={}",
                orderId, managerId, newStatus);
        if (newStatus != OrderStatus.REJECTED && newStatus != OrderStatus.APPROVED) {
            throw new ServiceException("Недопустимое состояние для этого заказа");
        }

        Order savedOrder = jtaTransactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
            assertManagedBy(order, managerId);
            assertStatusIsNew(order);

            order.setStatus(newStatus);
            order.setUpdatedAt(OffsetDateTime.now());
            Order updatedOrder = orderRepository.save(order);

            return updatedOrder;
        });

        if (savedOrder == null) {
            throw new ServiceException("Не удалось изменить статус заказа: " + orderId);
        }

        log.info("Order status changed: orderId={}, managerId={}, status={}",
                savedOrder.getId(), managerId, newStatus);
        if (newStatus == OrderStatus.APPROVED) {
            PaymentLinkRequested paymentRequest = new PaymentLinkRequested(
                    savedOrder.getId(),
                    UUID.randomUUID().toString());
            kafkaMessageProducer.send(KafkaTopics.PAYMENT_LINK_REQUESTED,
                    savedOrder.getId().toString(),
                    paymentRequest);
        }
        return savedOrder;
    }

    @Override
    public PaymentLinkCreated createPaymentLink(PaymentLinkRequested request) {
        UUID orderId = request.orderId();
        Order orderForPayment = orderRepository.findById(orderId)
                .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
        assertStatusIsApproved(orderForPayment);

        BigDecimal totalAmount = calculateTotalAmount(orderForPayment);
        log.info("Requesting payment link: orderId={}, amount={}, currency={}",
                request.orderId(),
                totalAmount,
                yooKassaProperties.currency());
        CreatePaymentResult paymentResult = yooKassaPaymentService.createPayment(
                orderForPayment,
                totalAmount,
                request.idempotenceKey());

        return new PaymentLinkCreated(
                request.orderId(),
                paymentResult.yooKassaPaymentId(),
                request.idempotenceKey(),
                totalAmount,
                yooKassaProperties.currency(),
                paymentResult.rawStatus(),
                paymentResult.confirmationUrl());
    }

    @Override
    public OrderPlaced placeOrder(PaymentLinkCreated paymentLink) {
        OrderPlaced orderPlaced = jtaTransactionTemplate.execute(status -> {
            UUID orderId = paymentLink.orderId();
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ServiceException("Заказ не найден: " + orderId));
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(paymentLink.orderId());
            if (existingPayment.isPresent()) {
                if (order.getStatus() != OrderStatus.PLACED) {
                    throw new ServiceException("Оплата существует, но заказ не имеет статус PLACED: "
                            + paymentLink.orderId());
                }
                Payment payment = existingPayment.get();
                return new OrderPlaced(order.getId(), payment.getConfirmationUrl(), payment.getAmount());
            }
            assertStatusIsApproved(order);

            Payment payment = new Payment();
            payment.setOrderId(paymentLink.orderId());
            payment.setYooKassaPaymentId(paymentLink.yooKassaPaymentId());
            payment.setIdempotenceKey(paymentLink.idempotenceKey());
            payment.setAmount(paymentLink.amount());
            payment.setCurrency(paymentLink.currency());
            payment.setStatus(mapYooKassaStatus(paymentLink.rawStatus()));
            payment.setConfirmationUrl(paymentLink.confirmationUrl());
            payment.setReturnUrl(yooKassaProperties.returnUrl());
            payment.setCreatedAt(OffsetDateTime.now());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.PLACED);
            order.setUpdatedAt(OffsetDateTime.now());
            Order savedOrder = orderRepository.saveAndFlush(order);
            return new OrderPlaced(savedOrder.getId(), paymentLink.confirmationUrl(), paymentLink.amount());
        });

        if (orderPlaced == null) {
            throw new ServiceException("Не удалось создать платёж для заказа: " + paymentLink.orderId());
        }
        log.info("Payment and order committed in XA transaction: orderId={}, paymentId={}",
                paymentLink.orderId(), paymentLink.yooKassaPaymentId());
        return orderPlaced;
    }

    private void replaceItems(Order order, List<OrderItemRequest> itemRequests) {
        order.getItems().clear();
        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ServiceException("Товар не найден: " + itemRequest.productId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.quantity());
            order.getItems().add(orderItem);
        }
    }

    private Customer upsertCustomer(String name, String phoneNumber, String email) {
        Customer customer = customerRepository.findByEmail(email).orElseGet(Customer::new);
        customer.setName(name);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    private User requireRole(UUID userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("Пользователь не найден: " + userId));
        if (user.getRole() != role) {
            throw new ServiceException("Пользователь " + userId + " должен иметь роль " + role);
        }
        return user;
    }

    private void assertManagedBy(Order order, UUID managerId) {
        if (order.getManager() == null || !order.getManager().getId().equals(managerId)) {
            throw new ServiceException("Заказ не назначен менеджеру: " + managerId);
        }
    }

    private void assertStatusIsNew(Order order) {
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ServiceException("Заказ можно изменять только в статусе NEW");
        }
    }

    private void assertStatusIsApproved(Order order) {
        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new ServiceException("Платёжную ссылку можно запросить только для заказа в статусе APPROVED");
        }
    }

    private BigDecimal calculateTotalAmount(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal lineAmount = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineAmount);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private YooKassaPaymentStatus mapYooKassaStatus(String status) {
        return switch (status) {
            case "pending" -> YooKassaPaymentStatus.PENDING;
            case "waiting_for_capture" -> YooKassaPaymentStatus.WAITING_FOR_CAPTURE;
            case "succeeded" -> YooKassaPaymentStatus.SUCCEEDED;
            case "canceled" -> YooKassaPaymentStatus.CANCELED;
            default -> throw new ServiceException("Неподдерживаемый статус YooKassa: " + status);
        };
    }

}
