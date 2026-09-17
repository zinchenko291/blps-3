package me.zinch.itmo.mts.eis;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.resource.ResourceException;

final class KillBillConnection implements BillingEisConnection {

    private final KillBillManagedConnection managedConnection;
    private final RestClient client;
    private final Map<String, String> plans;
    private boolean closed;

    KillBillConnection(
            KillBillManagedConnection managedConnection,
            RestClient client,
            Map<String, String> plans) {
        this.managedConnection = managedConnection;
        this.client = client;
        this.plans = plans;
    }

    @Override
    public ProvisionResult provisionServices(ProvisionServicesRequest request) throws ResourceException {
        ensureOpen();
        try {
            UUID accountId = findOrCreateAccount(request.customer());
            List<ProvisionResult.Subscription> subscriptions = new ArrayList<>();
            for (ProvisionServicesRequest.ServiceData service : request.services()) {
                String planName = plans.get(service.code());
                if (planName == null || planName.isBlank()) {
                    throw new ResourceException("Не настроен план Kill Bill для услуги " + service.code());
                }
                subscriptions
                        .add(new ProvisionResult.Subscription(service.code(), createSubscription(accountId, planName)));
            }
            return new ProvisionResult(true, accountId.toString(), subscriptions, null);
        } catch (ResourceException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ResourceException("Kill Bill API недоступен или отклонил запрос: " + exception.getMessage(),
                    exception);
        }
    }

    private UUID findOrCreateAccount(ProvisionServicesRequest.CustomerData customer) {
        KillBillAccount[] accounts;
        try {
            accounts = client.get()
                    .uri(builder -> builder.path("/1.0/kb/accounts")
                            .queryParam("externalKey", customer.externalId()).build())
                    .retrieve()
                    .body(KillBillAccount[].class);
        } catch (HttpClientErrorException.NotFound ignored) {
            accounts = null;
        }
        if (accounts != null && accounts.length > 0) {
            return UUID.fromString(accounts[0].accountId());
        }

        ResponseEntity<Void> response = client.post()
                .uri("/1.0/kb/accounts")
                .body(new CreateAccountRequest(customer.externalId(), customer.name(), customer.email()))
                .retrieve()
                .toBodilessEntity();
        return identifierFromLocation(response.getHeaders().getLocation(), "account");
    }

    private String createSubscription(UUID accountId, String planName) {
        ResponseEntity<Void> response = client.post()
                .uri("/1.0/kb/subscriptions")
                .body(new CreateSubscriptionRequest(accountId.toString(), planName))
                .retrieve()
                .toBodilessEntity();
        return identifierFromLocation(response.getHeaders().getLocation(), "subscription").toString();
    }

    private UUID identifierFromLocation(URI location, String resourceName) {
        if (location == null || location.getPath() == null) {
            throw new IllegalArgumentException("Kill Bill не вернул идентификатор " + resourceName);
        }
        String[] segments = location.getPath().split("/");
        return UUID.fromString(segments[segments.length - 1]);
    }

    private void ensureOpen() throws ResourceException {
        if (closed) {
            throw new ResourceException("Kill Bill connection is already closed");
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            managedConnection.close(this);
        }
    }

    private record KillBillAccount(String accountId) {
    }

    private record CreateAccountRequest(String externalKey, String name, String email) {
    }

    private record CreateSubscriptionRequest(String accountId, String planName) {
    }
}
