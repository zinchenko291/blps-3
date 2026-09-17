package me.zinch.itmo.mts.eis;

import jakarta.resource.ResourceException;

public interface BillingEisConnection extends AutoCloseable {

    ProvisionResult provisionServices(ProvisionServicesRequest request) throws ResourceException;

    @Override
    void close();
}
