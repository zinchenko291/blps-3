package me.zinch.itmo.mts.security;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

import me.zinch.itmo.mts.domain.enums.UserRole;
import me.zinch.itmo.mts.service.auth.UnauthorizedException;

@Service
public class XmlAccountStore {
    private static final String ACCOUNTS_FILE = "security/accounts.xml";
    private final List<StoredAccount> accounts = loadAccounts();

    public SecurityAccount byLogin(String login) {
        return findByLogin(login).account();
    }

    public SecurityAccount byId(UUID id) {
        StoredAccount account = accounts.stream()
                .filter(stored -> stored.account().id().equals(id))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Учётная запись не найдена"));
        return account.account();
    }

    public SecurityAccount authenticate(String login, char[] password) {
        StoredAccount account = findByLogin(login);
        if (!BCrypt.checkpw(new String(password), account.passwordHash())) {
            throw new UnauthorizedException("Неправильный пароль или логин");
        }
        return account.account();
    }

    public List<SecurityAccount> all() {
        return accounts.stream()
                .map(storedAccount -> storedAccount.account)
                .toList();
    }

    public String passwordHashById(UUID id) {
        return accounts.stream()
                .filter(stored -> stored.account().id().equals(id))
                .findFirst()
                .map(storedAccount -> storedAccount.passwordHash)
                .orElseThrow(() -> new IllegalArgumentException("Учётная запись не найдена: " + id));
    }

    private StoredAccount findByLogin(String login) {
        return accounts.stream()
                .filter(account -> account.account().login().equals(login))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Неправильный пароль или логин"));
    }

    private List<StoredAccount> loadAccounts() {
        try (var input = new ClassPathResource(ACCOUNTS_FILE).getInputStream()) {
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            var nodes = document.getDocumentElement().getElementsByTagName("account");
            var result = new ArrayList<StoredAccount>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element account = (Element) nodes.item(i);
                result.add(new StoredAccount(
                        new SecurityAccount(
                                UUID.fromString(account.getAttribute("id")),
                                account.getAttribute("login"),
                                account.getAttribute("name"),
                                UserRole.valueOf(account.getAttribute("role"))),
                        account.getAttribute("passwordHash")));
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось прочитать XML-файл учётных записей", exception);
        }
    }

    private record StoredAccount(SecurityAccount account, String passwordHash) {
    }
}
