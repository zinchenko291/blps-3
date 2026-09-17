package me.zinch.itmo.mts.service.order.message.consumer;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import me.zinch.itmo.mts.config.KafkaProperties;
import me.zinch.itmo.mts.service.order.OrderService;
import me.zinch.itmo.mts.service.order.message.KafkaTopics;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkCreated;
import me.zinch.itmo.mts.service.order.message.event.PaymentLinkRequested;
import me.zinch.itmo.mts.service.order.message.producer.KafkaMessageProducer;

@Component
public class PaymentLinkRequestConsumer extends AbstractKafkaConsumer<PaymentLinkRequested> {

    private final OrderService orderService;
    private final KafkaMessageProducer producer;

    public PaymentLinkRequestConsumer(KafkaProperties properties, ObjectMapper objectMapper,
            OrderService orderService, KafkaMessageProducer producer) {
        super(properties, objectMapper, KafkaTopics.PAYMENT_LINK_REQUESTED,
                "payment-link-worker", PaymentLinkRequested.class);
        this.orderService = orderService;
        this.producer = producer;
    }

    @Override
    protected void handle(PaymentLinkRequested request) {
        PaymentLinkCreated paymentLink = orderService.createPaymentLink(request);
        producer.send(KafkaTopics.PAYMENT_LINK_CREATED, request.orderId().toString(), paymentLink);
    }
}
