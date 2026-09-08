package com.achyut.operation.service;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.asset.*;
import com.achyut.operation.audit.*;
import com.achyut.operation.customer.*;
import com.achyut.operation.delivery.*;
import com.achyut.operation.inventory.*;
import com.achyut.operation.work.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationsService {
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkUpdateRepository workUpdateRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final AuditEventRepository auditEventRepository;

    public CustomerView createCustomer(CustomerRequest r) {
        Customer c = Customer.builder().name(r.name()).companyName(r.companyName()).phone(r.phone()).email(r.email()).billingAddress(r.billingAddress()).build();
        c = customerRepository.save(c);
        return customerView(c);
    }

    public List<CustomerView> customers() { return customerRepository.findAll().stream().map(this::customerView).toList(); }

    public AssetView createAsset(AssetRequest r) {
        Customer customer = customer(r.customerId());
        Asset a = Asset.builder().assetCode(r.assetCode()).type(r.type()).status(Asset.AssetStatus.RECEIVED).sizeDescription(r.sizeDescription()).serialNumber(r.serialNumber()).currentYardLocation(r.currentYardLocation()).customer(customer).build();
        a = assetRepository.save(a);
        audit("ASSET", a.getId(), "ASSET_CREATED", "system", a.getAssetCode() + " created");
        return assetView(a);
    }

    public List<AssetView> assets() { return assetRepository.findAll().stream().map(this::assetView).toList(); }

    public WorkOrderView createWorkOrder(WorkOrderRequest r) {
        Customer c = customer(r.customerId());
        Asset a = asset(r.assetId());
        if (!Objects.equals(a.getCustomer().getId(), c.getId())) throw new IllegalArgumentException("Asset does not belong to selected customer");
        WorkOrder w = WorkOrder.builder()
            .orderNumber(nextNumber("WO"))
            .customer(c).asset(a).workType(r.workType())
            .status(WorkOrder.WorkStatus.CREATED)
            .priority(r.priority() == null ? WorkOrder.Priority.NORMAL : r.priority())
            .scopeOfWork(r.scopeOfWork()).assignedTeam(r.assignedTeam())
            .estimatedCost(r.estimatedCost()).expectedCompletionAt(r.expectedCompletionAt()).progressPercent(0).build();
        w = workOrderRepository.save(w);
        audit("WORK_ORDER", w.getId(), "WORK_ORDER_CREATED", "system", w.getOrderNumber() + " for " + a.getAssetCode());
        return workOrderView(w);
    }

    public List<WorkOrderView> workOrders() { return workOrderRepository.findAll().stream().map(this::workOrderView).toList(); }

    public WorkOrderView updateWorkOrder(Long id, WorkOrderStatusRequest r) {
        WorkOrder w = workOrder(id);
        WorkOrder.WorkStatus old = w.getStatus();
        w.setStatus(r.status());
        if (r.progressPercent() != null) w.setProgressPercent(Math.max(0, Math.min(100, r.progressPercent())));
        w.setBlockedReason(r.status() == WorkOrder.WorkStatus.BLOCKED ? r.blockedReason() : null);
        if (r.approvedCost() != null) w.setApprovedCost(r.approvedCost());
        if (r.status() == WorkOrder.WorkStatus.COMPLETED) { w.setProgressPercent(100); w.setActualCompletionAt(Instant.now()); }
        syncAssetFromWorkOrder(w);
        audit("WORK_ORDER", id, "STATUS_CHANGED", actor(r.actor()), old + " -> " + r.status() + (r.blockedReason() == null ? "" : " | " + r.blockedReason()));
        return workOrderView(w);
    }

    public WorkUpdateView addWorkUpdate(Long workOrderId, WorkUpdateRequest r) {
        WorkOrder w = workOrder(workOrderId);
        WorkUpdate u = WorkUpdate.builder().workOrder(w).stage(r.stage()).status(r.status()).assignedTo(r.assignedTo()).note(r.note()).photoUrl(r.photoUrl()).build();
        u = workUpdateRepository.save(u);
        if (r.status() == WorkUpdate.StageStatus.IN_PROGRESS && w.getStatus() != WorkOrder.WorkStatus.BLOCKED) w.setStatus(WorkOrder.WorkStatus.IN_PROGRESS);
        audit("WORK_ORDER", workOrderId, "STAGE_UPDATE", actor(r.actor()), r.stage() + " -> " + r.status() + (r.note() == null ? "" : " | " + r.note()));
        return workUpdateView(u);
    }

    public List<WorkUpdateView> workUpdates(Long workOrderId) {
        workOrder(workOrderId);
        return workUpdateRepository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId).stream().map(this::workUpdateView).toList();
    }

    public InventoryView createInventory(InventoryRequest r) {
        InventoryItem item = InventoryItem.builder().sku(r.sku()).name(r.name()).unit(r.unit()).quantityOnHand(r.quantityOnHand()).reorderLevel(r.reorderLevel()).unitCost(r.unitCost()).preferredSupplier(r.preferredSupplier()).build();
        return inventoryView(inventoryItemRepository.save(item));
    }

    public List<InventoryView> inventory() { return inventoryItemRepository.findAll().stream().map(this::inventoryView).toList(); }

    public InventoryView adjustInventory(Long id, InventoryAdjustmentRequest r) {
        InventoryItem item = inventoryItemRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Inventory item not found"));
        BigDecimal current = item.getQuantityOnHand() == null ? BigDecimal.ZERO : item.getQuantityOnHand();
        BigDecimal next = current.add(r.delta());
        if (next.signum() < 0) throw new IllegalArgumentException("Inventory cannot become negative");
        item.setQuantityOnHand(next);
        audit("INVENTORY", id, "STOCK_ADJUSTED", actor(r.actor()), r.delta() + " " + item.getUnit() + (r.reason() == null ? "" : " | " + r.reason()));
        return inventoryView(item);
    }

    public DeliveryView createDelivery(DeliveryRequest r) {
        WorkOrder w = workOrder(r.workOrderId());
        Asset a = asset(r.assetId());
        if (!Objects.equals(w.getAsset().getId(), a.getId())) throw new IllegalArgumentException("Work order and asset do not match");
        Delivery d = Delivery.builder().deliveryNumber(nextNumber("DEL")).workOrder(w).asset(a).status(Delivery.DeliveryStatus.PLANNED)
            .driverName(r.driverName()).driverPhone(r.driverPhone()).vehicleNumber(r.vehicleNumber()).vehicleType(r.vehicleType())
            .destinationLatitude(r.destinationLatitude()).destinationLongitude(r.destinationLongitude()).destinationAddress(r.destinationAddress()).expectedDeliveryAt(r.expectedDeliveryAt()).build();
        d = deliveryRepository.save(d);
        audit("DELIVERY", d.getId(), "DELIVERY_CREATED", "system", d.getDeliveryNumber() + " for " + a.getAssetCode());
        return deliveryView(d);
    }

    public List<DeliveryView> deliveries() { return deliveryRepository.findAll().stream().map(this::deliveryView).toList(); }

    public DeliveryView updateDelivery(Long id, DeliveryStatusRequest r) {
        Delivery d = delivery(id);
        Delivery.DeliveryStatus old = d.getStatus();
        d.setStatus(r.status());
        if (r.status() == Delivery.DeliveryStatus.DISPATCHED && d.getDispatchedAt() == null) d.setDispatchedAt(Instant.now());
        if (r.status() == Delivery.DeliveryStatus.DELIVERED || r.status() == Delivery.DeliveryStatus.INSTALLED) {
            if (d.getDeliveredAt() == null) d.setDeliveredAt(Instant.now());
            if (r.proofOfDeliveryUrl() != null) d.setProofOfDeliveryUrl(r.proofOfDeliveryUrl());
            if (r.receivedBy() != null) d.setReceivedBy(r.receivedBy());
        }
        syncAssetFromDelivery(d);
        audit("DELIVERY", id, "STATUS_CHANGED", actor(r.actor()), old + " -> " + r.status());
        return deliveryView(d);
    }

    public DashboardView dashboard() {
        List<InventoryItem> items = inventoryItemRepository.findAll();
        long lowStock = items.stream().filter(InventoryItem::isLowStock).count();
        List<WorkOrder> overdue = workOrderRepository.findByExpectedCompletionAtBeforeAndStatusNotIn(Instant.now(), List.of(WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.CANCELLED));
        List<AlertView> alerts = new ArrayList<>();
        overdue.stream().limit(10).forEach(w -> alerts.add(new AlertView("HIGH", "OVERDUE_WORK", w.getOrderNumber(), "Expected completion has passed for " + w.getAsset().getAssetCode())));
        items.stream().filter(InventoryItem::isLowStock).limit(10).forEach(i -> alerts.add(new AlertView("MEDIUM", "LOW_STOCK", i.getSku(), i.getName() + " is at or below reorder level")));
        deliveryRepository.findAll().stream().filter(d -> d.getExpectedDeliveryAt() != null && d.getExpectedDeliveryAt().isBefore(Instant.now()) && !List.of(Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.INSTALLED, Delivery.DeliveryStatus.CANCELLED).contains(d.getStatus())).limit(10)
            .forEach(d -> alerts.add(new AlertView("HIGH", "DELIVERY_DELAY", d.getDeliveryNumber(), "Delivery ETA has passed for " + d.getAsset().getAssetCode())));
        long active = workOrderRepository.findAll().stream().filter(w -> !List.of(WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.CANCELLED).contains(w.getStatus())).count();
        return new DashboardView(customerRepository.count(), assetRepository.count(), assetRepository.countByType(Asset.AssetType.CONTAINER), assetRepository.countByType(Asset.AssetType.PORTA_CABIN), workOrderRepository.count(), active, workOrderRepository.countByStatus(WorkOrder.WorkStatus.READY_FOR_DISPATCH), deliveryRepository.countByStatus(Delivery.DeliveryStatus.IN_TRANSIT), deliveryRepository.countByStatus(Delivery.DeliveryStatus.DELAYED), lowStock, alerts);
    }

    public List<AuditEvent> timeline(String type, Long id) { return auditEventRepository.findTop100ByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(type.toUpperCase(Locale.ROOT), id); }

    public Delivery delivery(Long id) { return deliveryRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Delivery not found: " + id)); }
    public Asset asset(Long id) { return assetRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Asset not found: " + id)); }
    public WorkOrder workOrder(Long id) { return workOrderRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Work order not found: " + id)); }
    private Customer customer(Long id) { return customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Customer not found: " + id)); }

    public DeliveryView deliveryView(Delivery d) { return new DeliveryView(d.getId(), d.getDeliveryNumber(), d.getWorkOrder().getOrderNumber(), d.getAsset().getAssetCode(), d.getStatus(), d.getDriverName(), d.getDriverPhone(), d.getVehicleNumber(), d.getDestinationLatitude(), d.getDestinationLongitude(), d.getDestinationAddress(), d.getExpectedDeliveryAt(), d.getDispatchedAt(), d.getDeliveredAt(), d.getProofOfDeliveryUrl(), d.getReceivedBy()); }
    private CustomerView customerView(Customer c) { return new CustomerView(c.getId(), c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getBillingAddress()); }
    private AssetView assetView(Asset a) { return new AssetView(a.getId(), a.getAssetCode(), a.getType(), a.getStatus(), a.getSizeDescription(), a.getCurrentYardLocation(), a.getCustomer().getId(), a.getCustomer().getName()); }
    private WorkOrderView workOrderView(WorkOrder w) { return new WorkOrderView(w.getId(), w.getOrderNumber(), w.getCustomer().getName(), w.getAsset().getAssetCode(), w.getWorkType(), w.getStatus(), w.getPriority(), w.getAssignedTeam(), w.getBlockedReason(), w.getProgressPercent() == null ? 0 : w.getProgressPercent(), w.getEstimatedCost(), w.getApprovedCost(), w.getExpectedCompletionAt(), w.getActualCompletionAt()); }
    private WorkUpdateView workUpdateView(WorkUpdate u) { return new WorkUpdateView(u.getId(), u.getStage(), u.getStatus(), u.getAssignedTo(), u.getNote(), u.getPhotoUrl(), u.getCreatedAt()); }
    private InventoryView inventoryView(InventoryItem i) { return new InventoryView(i.getId(), i.getSku(), i.getName(), i.getUnit(), i.getQuantityOnHand(), i.getReorderLevel(), i.getUnitCost(), i.getPreferredSupplier(), i.isLowStock()); }

    private void syncAssetFromWorkOrder(WorkOrder w) {
        Asset a = w.getAsset();
        switch (w.getStatus()) {
            case INSPECTION_PENDING, ESTIMATE_PENDING, CUSTOMER_APPROVAL_PENDING -> a.setStatus(Asset.AssetStatus.INSPECTION);
            case IN_PROGRESS -> a.setStatus(w.getWorkType() == WorkOrder.WorkType.PORTA_CABIN_PRODUCTION ? Asset.AssetStatus.PRODUCTION_IN_PROGRESS : Asset.AssetStatus.REPAIR_IN_PROGRESS);
            case QUALITY_CHECK -> a.setStatus(Asset.AssetStatus.QUALITY_CHECK);
            case READY_FOR_DISPATCH, COMPLETED -> a.setStatus(Asset.AssetStatus.READY_FOR_DISPATCH);
            case BLOCKED -> a.setStatus(Asset.AssetStatus.ON_HOLD);
            default -> { }
        }
    }

    private void syncAssetFromDelivery(Delivery d) {
        Asset a = d.getAsset();
        switch (d.getStatus()) {
            case DISPATCHED -> a.setStatus(Asset.AssetStatus.DISPATCHED);
            case IN_TRANSIT, DELAYED, NEAR_DESTINATION -> a.setStatus(Asset.AssetStatus.IN_TRANSIT);
            case DELIVERED, INSTALLATION_IN_PROGRESS -> a.setStatus(Asset.AssetStatus.DELIVERED);
            case INSTALLED -> a.setStatus(Asset.AssetStatus.INSTALLED);
            default -> { }
        }
    }

    public void audit(String type, Long id, String action, String actor, String details) {
        auditEventRepository.save(AuditEvent.builder().referenceType(type).referenceId(id).action(action).actor(actor).details(details).build());
    }

    private String actor(String actor) { return actor == null || actor.isBlank() ? "system" : actor; }
    private String nextNumber(String prefix) { return prefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT); }
}
