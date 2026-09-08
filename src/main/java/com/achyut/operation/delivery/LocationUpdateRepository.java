package com.achyut.operation.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationUpdateRepository extends JpaRepository<LocationUpdate, Long> {
    Optional<LocationUpdate> findTopByDeliveryIdOrderByRecordedAtDesc(Long deliveryId);
    List<LocationUpdate> findTop100ByDeliveryIdOrderByRecordedAtDesc(Long deliveryId);
}
