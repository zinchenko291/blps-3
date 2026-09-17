package me.zinch.itmo.mts.eis;

import java.io.PrintWriter;
import java.util.Objects;
import java.util.Set;

import javax.security.auth.Subject;

import org.springframework.web.client.RestClient;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import lombok.Setter;

@Setter
public class KillBillManagedConnectionFactory implements ManagedConnectionFactory {

    private KillBillProperties properties;
    private ResourceAdapter resourceAdapter;
    private PrintWriter logWriter;

    @Override
    public BillingEisConnectionFactory createConnectionFactory(ConnectionManager connectionManager) {
        return new KillBillConnectionFactory(this);
    }

    @Override
    public BillingEisConnectionFactory createConnectionFactory() {
        return new KillBillConnectionFactory(this);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        if (properties == null) {
            throw new ResourceException("Kill Bill adapter is not configured");
        }
        RestClient client = RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.getUsername(), properties.getPassword());
                    headers.set("X-Killbill-ApiKey", properties.getApiKey());
                    headers.set("X-Killbill-ApiSecret", properties.getApiSecret());
                    headers.set("X-Killbill-CreatedBy", "mts-crm");
                })
                .build();
        return new KillBillManagedConnection(client, properties.getPlans());
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject,
            ConnectionRequestInfo connectionRequestInfo) {
        return null;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        logWriter = out;
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return Objects.hash(KillBillManagedConnectionFactory.class);
    }
}
