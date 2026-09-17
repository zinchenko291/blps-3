package me.zinch.itmo.mts.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import me.zinch.itmo.mts.domain.entity.Order;
import me.zinch.itmo.mts.domain.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Override
    @EntityGraph(attributePaths = { "customer", "manager", "items", "items.product" })
    Page<Order> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "customer", "manager", "items", "items.product" })
    Optional<Order> findById(UUID id);

    @EntityGraph(attributePaths = { "customer", "manager", "items", "items.product" })
    Page<Order> findAllByManagerId(UUID managerId, Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(OffsetDateTime from, OffsetDateTime to);

    @EntityGraph(attributePaths = { "items", "items.product" })
    List<Order> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndStatus(
            OffsetDateTime from, OffsetDateTime to, OrderStatus status);
}
