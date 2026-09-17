package me.zinch.itmo.mts.service.order.message.consumer;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.zinch.itmo.mts.config.KafkaProperties;
import me.zinch.itmo.mts.repository.OrderRepository;
import me.zinch.itmo.mts.service.ServiceException;
import me.zinch.itmo.mts.service.notification.OrderEmailService;
import me.zinch.itmo.mts.service.order.message.KafkaTopics;
import me.zinch.itmo.mts.service.order.message.event.OrderPlaced;

@Component
public class OrderPlacedConsumer extends AbstractKafkaConsumer<OrderPlaced> {

    private final OrderRepository orderRepository;
    private final OrderEmailService orderEmailService;
    private final TransactionTemplate jtaTransactionTemplate;

    public OrderPlacedConsumer(
            KafkaProperties properties,
            ObjectMapper objectMapper,
            OrderRepository orderRepository,
            OrderEmailService orderEmailService,
            TransactionTemplate jtaTransactionTemplate) {
        super(
                properties,
                objectMapper,
                KafkaTopics.ORDER_PLACED,
                "email-worker",
                OrderPlaced.class);
        this.orderRepository = orderRepository;
        this.orderEmailService = orderEmailService;
        this.jtaTransactionTemplate = jtaTransactionTemplate;
    }

    @Override
    protected void handle(OrderPlaced event) {
        jtaTransactionTemplate.executeWithoutResult(status -> orderEmailService.sendOrderPlacedEmail(
                orderRepository.findById(event.orderId())
                        .orElseThrow(() -> new ServiceException("Заказ не найден: " + event.orderId())),
                event.paymentUrl(), event.totalAmount()));
    }
}
