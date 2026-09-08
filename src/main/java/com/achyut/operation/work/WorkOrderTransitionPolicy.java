package com.achyut.operation.work;

import com.achyut.operation.common.TransitionPolicy;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WorkOrderTransitionPolicy implements TransitionPolicy<WorkOrder.WorkStatus> {
    private final Map<WorkOrder.WorkStatus, Set<WorkOrder.WorkStatus>> allowed = new EnumMap<>(WorkOrder.WorkStatus.class);

    public WorkOrderTransitionPolicy() {
        allow(WorkOrder.WorkStatus.CREATED, WorkOrder.WorkStatus.INSPECTION_PENDING, WorkOrder.WorkStatus.ESTIMATE_PENDING, WorkOrder.WorkStatus.CUSTOMER_APPROVAL_PENDING, WorkOrder.WorkStatus.APPROVED, WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.INSPECTION_PENDING, WorkOrder.WorkStatus.ESTIMATE_PENDING, WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.ESTIMATE_PENDING, WorkOrder.WorkStatus.CUSTOMER_APPROVAL_PENDING, WorkOrder.WorkStatus.APPROVED, WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.CUSTOMER_APPROVAL_PENDING, WorkOrder.WorkStatus.APPROVED, WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.APPROVED, WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.QUALITY_CHECK, WorkOrder.WorkStatus.READY_FOR_DISPATCH, WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.QUALITY_CHECK, WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.READY_FOR_DISPATCH, WorkOrder.WorkStatus.BLOCKED, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.READY_FOR_DISPATCH, WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.CANCELLED);
        allow(WorkOrder.WorkStatus.COMPLETED);
        allow(WorkOrder.WorkStatus.CANCELLED);
    }

    @Override
    public void validate(WorkOrder.WorkStatus current, WorkOrder.WorkStatus target) {
        if (current == target) return;
        if (!allowed.getOrDefault(current, Set.of()).contains(target)) {
            throw new IllegalStateException("Invalid work-order transition: " + current + " -> " + target);
        }
    }

    private void allow(WorkOrder.WorkStatus from, WorkOrder.WorkStatus... targets) {
        allowed.put(from, targets.length == 0 ? EnumSet.noneOf(WorkOrder.WorkStatus.class) : EnumSet.copyOf(List.of(targets)));
    }
}
