package me.zinch.itmo.mts.eis;

import java.io.Serializable;

import jakarta.resource.ResourceException;

public interface BillingEisConnectionFactory extends Serializable {

    BillingEisConnection getConnection() throws ResourceException;
}
