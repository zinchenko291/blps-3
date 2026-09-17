package me.zinch.itmo.mts.service.order.message;

public final class KafkaTopics {

    public static final String PAYMENT_LINK_REQUESTED = "order.payment-link-requested";
    public static final String PAYMENT_LINK_CREATED = "order.payment-link-created";
    public static final String ORDER_PLACED = "order.placed";

    private KafkaTopics() {
    }
}
