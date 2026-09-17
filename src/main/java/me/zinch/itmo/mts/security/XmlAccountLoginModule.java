package me.zinch.itmo.mts.security;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class XmlAccountLoginModule implements LoginModule {
    private Subject subject;
    private CallbackHandler callbackHandler;
    private XmlAccountStore accountStore;
    private SecurityAccount account;

    @Override
    public void initialize(
            Subject subject,
            CallbackHandler callbackHandler,
            Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        Object store = options.get("accountStore");
        if (store instanceof XmlAccountStore value) {
            this.accountStore = value;
        }
    }

    @Override
    public boolean login() throws LoginException {
        var name = new NameCallback("login");
        var password = new PasswordCallback("password", false);
        try {
            callbackHandler.handle(new Callback[] { name, password });
            if (accountStore == null) {
                throw new FailedLoginException("Не настроено хранилище учётных записей");
            }
            account = accountStore.authenticate(name.getName(), password.getPassword());
        } catch (Exception exception) {
            throw new FailedLoginException("Не удалось проверить учётную запись");
        }
        return true;
    }

    @Override
    public boolean commit() {
        subject.getPrincipals().add(new AccountPrincipal(account));
        return true;
    }

    @Override
    public boolean abort() {
        account = null;
        return true;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().clear();
        return true;
    }

    public record AccountPrincipal(SecurityAccount account) implements Principal {
        @Override
        public String getName() {
            return account.login();
        }
    }
}
