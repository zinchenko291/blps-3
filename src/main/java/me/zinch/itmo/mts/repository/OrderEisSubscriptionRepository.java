package me.zinch.itmo.mts.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import me.zinch.itmo.mts.domain.entity.OrderEisSubscription;

public interface OrderEisSubscriptionRepository extends JpaRepository<OrderEisSubscription, UUID> {

    void deleteAllByOrderId(UUID orderId);
}
