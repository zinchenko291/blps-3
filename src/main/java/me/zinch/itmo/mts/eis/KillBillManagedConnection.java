package me.zinch.itmo.mts.eis;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.springframework.web.client.RestClient;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

final class KillBillManagedConnection implements ManagedConnection {

    private final RestClient client;
    private final Map<String, String> plans;
    private final List<ConnectionEventListener> listeners = new ArrayList<>();
    private PrintWriter logWriter;
    private boolean destroyed;

    KillBillManagedConnection(RestClient client, Map<String, String> plans) {
        this.client = client;
        this.plans = Map.copyOf(plans);
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        ensureOpen();
        return new KillBillConnection(this, client, plans);
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof KillBillConnection)) {
            throw new ResourceException("Connection was not created by Kill Bill adapter");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new ResourceException("Kill Bill HTTP adapter does not expose an XA resource");
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new ResourceException("Kill Bill HTTP adapter does not expose local transactions");
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new KillBillManagedConnectionMetaData();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    void close(KillBillConnection connection) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);
        listeners.forEach(listener -> listener.connectionClosed(event));
    }

    private void ensureOpen() throws ResourceException {
        if (destroyed) {
            throw new ResourceException("Kill Bill managed connection is closed");
        }
    }

    private static final class KillBillManagedConnectionMetaData implements ManagedConnectionMetaData {
        @Override
        public String getEISProductName() {
            return "Kill Bill";
        }

        @Override
        public String getEISProductVersion() {
            return "0.24";
        }

        @Override
        public int getMaxConnections() {
            return 1;
        }

        @Override
        public String getUserName() {
            return "mts-crm";
        }
    }
}
