package me.zinch.itmo.mts.service.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import me.zinch.itmo.mts.domain.entity.User;

public interface ManagerService {
    Page<User> getManagersForSenior(Pageable pageable);
}
