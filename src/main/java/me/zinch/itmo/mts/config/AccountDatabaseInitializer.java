package me.zinch.itmo.mts.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.domain.entity.User;
import me.zinch.itmo.mts.repository.UserRepository;
import me.zinch.itmo.mts.security.SecurityAccount;
import me.zinch.itmo.mts.security.XmlAccountStore;

@Component
@RequiredArgsConstructor
public class AccountDatabaseInitializer implements ApplicationRunner {

    private final XmlAccountStore xmlAccountStore;
    private final UserRepository userRepository;
    private final TransactionTemplate jtaTransactionTemplate;

    @Override
    public void run(ApplicationArguments arguments) {
        jtaTransactionTemplate.executeWithoutResult(status -> xmlAccountStore.all().forEach(this::createIfMissing));
    }

    private void createIfMissing(SecurityAccount account) {
        if (userRepository.existsById(account.id())) {
            return;
        }
        userRepository.save(User.builder()
                .id(account.id())
                .login(account.login())
                .password(xmlAccountStore.passwordHashById(account.id()))
                .name(account.name())
                .role(account.role())
                .build());
    }
}
