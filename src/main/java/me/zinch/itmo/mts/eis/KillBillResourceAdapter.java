package me.zinch.itmo.mts.eis;

import java.io.Serializable;

import javax.transaction.xa.XAResource;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

public class KillBillResourceAdapter implements ResourceAdapter, Serializable {

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec)
            throws ResourceException {
        throw new ResourceException("Kill Bill adapter supports outbound calls only");
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) {
        return new XAResource[0];
    }
}
