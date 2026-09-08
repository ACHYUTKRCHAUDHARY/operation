package com.achyut.operation.work;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkUpdateRepository extends JpaRepository<WorkUpdate, Long> {
    List<WorkUpdate> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);
}
