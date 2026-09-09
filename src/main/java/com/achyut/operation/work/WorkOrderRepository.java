package com.achyut.operation.work;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByOrderNumber(String orderNumber);
    long countByStatus(WorkOrder.WorkStatus status);

    @Override
    @EntityGraph(attributePaths = {"customer", "asset"})
    List<WorkOrder> findAll();

    @EntityGraph(attributePaths = {"asset"})
    List<WorkOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("select count(w) from WorkOrder w where w.status not in :statuses")
    long countByStatusNotIn(@Param("statuses") List<WorkOrder.WorkStatus> statuses);

    @EntityGraph(attributePaths = {"asset"})
    @Query("select w from WorkOrder w where w.expectedCompletionAt < :cutoff and w.status not in :statuses order by w.expectedCompletionAt asc")
    List<WorkOrder> findOverdueForDashboard(@Param("cutoff") Instant cutoff,
                                            @Param("statuses") List<WorkOrder.WorkStatus> statuses,
                                            Pageable pageable);

    List<WorkOrder> findByExpectedCompletionAtBeforeAndStatusNotIn(Instant cutoff, List<WorkOrder.WorkStatus> statuses);
}
