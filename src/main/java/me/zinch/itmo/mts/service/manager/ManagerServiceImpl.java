package me.zinch.itmo.mts.service.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import me.zinch.itmo.mts.domain.entity.User;
import me.zinch.itmo.mts.domain.enums.UserRole;
import me.zinch.itmo.mts.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final UserRepository userRepository;
    private final TransactionTemplate jtaTransactionTemplate;

    @Override
    @PreAuthorize("hasAuthority('MANAGER_LIST')")
    public Page<User> getManagersForSenior(Pageable pageable) {
        return jtaTransactionTemplate.execute(status -> userRepository.findAllByRole(UserRole.MANAGER, pageable));
    }
}
