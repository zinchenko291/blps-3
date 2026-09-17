package me.zinch.itmo.mts.eis;

import jakarta.resource.ResourceException;

final class KillBillConnectionFactory implements BillingEisConnectionFactory {

    private final KillBillManagedConnectionFactory managedConnectionFactory;

    KillBillConnectionFactory(KillBillManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
    }

    @Override
    public BillingEisConnection getConnection() throws ResourceException {
        KillBillManagedConnection managedConnection = (KillBillManagedConnection) managedConnectionFactory
                .createManagedConnection(null, null);
        return (BillingEisConnection) managedConnection.getConnection(null, null);
    }
}
