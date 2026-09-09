package com.achyut.operation.delivery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByDeliveryNumber(String deliveryNumber);

    @Override
    @EntityGraph(attributePaths = {"workOrder", "asset"})
    List<Delivery> findAll();

    @EntityGraph(attributePaths = {"asset"})
    Optional<Delivery> findByPublicTrackingToken(String publicTrackingToken);

    @EntityGraph(attributePaths = {"workOrder", "asset"})
    List<Delivery> findByWorkOrderCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByStatus(Delivery.DeliveryStatus status);

    @EntityGraph(attributePaths = {"asset"})
    @Query("select d from Delivery d where d.expectedDeliveryAt < :cutoff and d.status not in :statuses order by d.expectedDeliveryAt asc")
    List<Delivery> findOverdueForDashboard(@Param("cutoff") Instant cutoff,
                                           @Param("statuses") List<Delivery.DeliveryStatus> statuses,
                                           Pageable pageable);
}
