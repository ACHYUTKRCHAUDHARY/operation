package com.achyut.operation.delivery;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);
    @EntityGraph(attributePaths = {"asset"})
    Optional<Delivery> findByPublicTrackingToken(String publicTrackingToken);
    long countByStatus(Delivery.DeliveryStatus status);
}
