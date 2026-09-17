package me.zinch.itmo.mts.eis;

import java.util.List;

public record ProvisionResult(boolean success, String externalCustomerId, List<Subscription> subscriptions,
        String errorMessage) {

    public record Subscription(String serviceCode, String externalSubscriptionId) {
    }
}
