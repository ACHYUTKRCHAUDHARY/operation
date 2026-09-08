package com.achyut.operation.work;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderTransitionPolicyTest {
    private final WorkOrderTransitionPolicy policy = new WorkOrderTransitionPolicy();

    @Test
    void allowsExpectedProgression() {
        assertDoesNotThrow(() -> policy.validate(WorkOrder.WorkStatus.CREATED, WorkOrder.WorkStatus.INSPECTION_PENDING));
        assertDoesNotThrow(() -> policy.validate(WorkOrder.WorkStatus.IN_PROGRESS, WorkOrder.WorkStatus.QUALITY_CHECK));
        assertDoesNotThrow(() -> policy.validate(WorkOrder.WorkStatus.READY_FOR_DISPATCH, WorkOrder.WorkStatus.COMPLETED));
    }

    @Test
    void rejectsInvalidJumpFromCompleted() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> policy.validate(WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.IN_PROGRESS));
        assertTrue(ex.getMessage().contains("Invalid work-order transition"));
    }
}
