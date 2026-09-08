package com.achyut.operation.work;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByOrderNumber(String orderNumber);
    long countByStatus(WorkOrder.WorkStatus status);
    List<WorkOrder> findByExpectedCompletionAtBeforeAndStatusNotIn(Instant cutoff, List<WorkOrder.WorkStatus> statuses);
}
