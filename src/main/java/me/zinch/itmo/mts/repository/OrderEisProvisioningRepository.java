package me.zinch.itmo.mts.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import me.zinch.itmo.mts.domain.entity.OrderEisProvisioning;

public interface OrderEisProvisioningRepository extends JpaRepository<OrderEisProvisioning, UUID> {
}
