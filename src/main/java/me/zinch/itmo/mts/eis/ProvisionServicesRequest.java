package me.zinch.itmo.mts.eis;

import java.util.List;

public record ProvisionServicesRequest(String orderId, CustomerData customer, List<ServiceData> services) {

    public record CustomerData(String externalId, String name, String email) {
    }

    public record ServiceData(String code, String name) {
    }
}
