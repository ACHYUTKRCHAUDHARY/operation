package com.achyut.operation.work;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.api.OperationsMapper;
import com.achyut.operation.asset.*;
import com.achyut.operation.common.*;
import com.achyut.operation.customer.*;
import com.achyut.operation.service.usecase.WorkOrderOperations;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkOrderService implements WorkOrderOperations {
    private final WorkOrderRepository workOrders;
    private final WorkUpdateRepository workUpdates;
    private final CustomerRepository customers;
    private final AssetRepository assets;
    private final OperationsMapper mapper;
    private final ReferenceNumberGenerator numbers;
    private final AuditPort audit;
    private final WorkOrderAssetStatusPolicy assetStatusPolicy;

    @Override
    public WorkOrderView create(WorkOrderRequest r) {
        Customer customer = customers.findById(r.customerId()).orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Asset asset = assets.findById(r.assetId()).orElseThrow(() -> new NoSuchElementException("Asset not found"));
        if (!Objects.equals(asset.getCustomer().getId(), customer.getId())) throw new IllegalArgumentException("Asset does not belong to selected customer");
        WorkOrder workOrder = WorkOrder.builder().orderNumber(numbers.next("WO")).customer(customer).asset(asset).workType(r.workType())
            .status(WorkOrder.WorkStatus.CREATED).priority(r.priority() == null ? WorkOrder.Priority.NORMAL : r.priority())
            .scopeOfWork(r.scopeOfWork()).assignedTeam(r.assignedTeam()).estimatedCost(r.estimatedCost())
            .expectedCompletionAt(r.expectedCompletionAt()).progressPercent(0).build();
        workOrder = workOrders.save(workOrder);
        audit.record("WORK_ORDER", workOrder.getId(), "WORK_ORDER_CREATED", "system", workOrder.getOrderNumber() + " for " + asset.getAssetCode());
        return mapper.workOrder(workOrder);
    }

    @Override
    public List<WorkOrderView> list() { return workOrders.findAll().stream().map(mapper::workOrder).toList(); }

    @Override
    public WorkOrderView update(Long id, WorkOrderStatusRequest r) {
        WorkOrder workOrder = find(id);
        WorkOrder.WorkStatus old = workOrder.getStatus();
        workOrder.setStatus(r.status());
        if (r.progressPercent() != null) workOrder.setProgressPercent(Math.max(0, Math.min(100, r.progressPercent())));
        workOrder.setBlockedReason(r.status() == WorkOrder.WorkStatus.BLOCKED ? r.blockedReason() : null);
        if (r.approvedCost() != null) workOrder.setApprovedCost(r.approvedCost());
        if (r.status() == WorkOrder.WorkStatus.COMPLETED) {
            workOrder.setProgressPercent(100);
            workOrder.setActualCompletionAt(Instant.now());
        }
        assetStatusPolicy.synchronize(workOrder);
        audit.record("WORK_ORDER", id, "STATUS_CHANGED", actor(r.actor()), old + " -> " + r.status() + (r.blockedReason() == null ? "" : " | " + r.blockedReason()));
        return mapper.workOrder(workOrder);
    }

    @Override
    public WorkUpdateView addUpdate(Long id, WorkUpdateRequest r) {
        WorkOrder workOrder = find(id);
        WorkUpdate update = workUpdates.save(WorkUpdate.builder().workOrder(workOrder).stage(r.stage()).status(r.status())
            .assignedTo(r.assignedTo()).note(r.note()).photoUrl(r.photoUrl()).build());
        if (r.status() == WorkUpdate.StageStatus.IN_PROGRESS && workOrder.getStatus() != WorkOrder.WorkStatus.BLOCKED) {
            workOrder.setStatus(WorkOrder.WorkStatus.IN_PROGRESS);
            assetStatusPolicy.synchronize(workOrder);
        }
        audit.record("WORK_ORDER", id, "STAGE_UPDATE", actor(r.actor()), r.stage() + " -> " + r.status() + (r.note() == null ? "" : " | " + r.note()));
        return mapper.workUpdate(update);
    }

    @Override
    public List<WorkUpdateView> updates(Long id) {
        find(id);
        return workUpdates.findByWorkOrderIdOrderByCreatedAtAsc(id).stream().map(mapper::workUpdate).toList();
    }

    private WorkOrder find(Long id) { return workOrders.findById(id).orElseThrow(() -> new NoSuchElementException("Work order not found: " + id)); }
    private String actor(String actor) { return actor == null || actor.isBlank() ? "system" : actor; }
}
