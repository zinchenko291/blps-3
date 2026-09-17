package me.zinch.itmo.mts.service.order.message.consumer;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.zinch.itmo.mts.config.KafkaProperties;
import me.zinch.itmo.mts.service.order.OrderService;
import me.zinch.itmo.mts.service.order.message.KafkaTopics;
import me.zinch.itmo.mts.service.order.message.event.OrderPlaced;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkCreated;
import me.zinch.itmo.mts.service.order.message.producer.KafkaMessageProducer;

@Component
public class PaymentLinkCreatedConsumer extends AbstractKafkaConsumer<PaymentLinkCreated> {

    private final OrderService orderService;
    private final KafkaMessageProducer producer;

    public PaymentLinkCreatedConsumer(
            KafkaProperties properties,
            ObjectMapper objectMapper,
            OrderService orderService,
            KafkaMessageProducer producer) {
        super(properties, objectMapper, KafkaTopics.PAYMENT_LINK_CREATED,
                "order-placement-worker", PaymentLinkCreated.class);
        this.orderService = orderService;
        this.producer = producer;
    }

    @Override
    protected void handle(PaymentLinkCreated paymentLink) {
        OrderPlaced orderPlaced = orderService.placeOrder(paymentLink);
        producer.send(KafkaTopics.ORDER_PLACED, paymentLink.orderId().toString(), orderPlaced);
    }
}
