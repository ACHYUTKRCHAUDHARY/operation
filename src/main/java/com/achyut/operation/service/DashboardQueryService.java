package com.achyut.operation.service;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.asset.*;
import com.achyut.operation.delivery.*;
import com.achyut.operation.inventory.*;
import com.achyut.operation.service.usecase.DashboardQuery;
import com.achyut.operation.work.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService implements DashboardQuery {
    private final com.achyut.operation.customer.CustomerRepository customers;
    private final AssetRepository assets;
    private final WorkOrderRepository workOrders;
    private final InventoryItemRepository inventory;
    private final DeliveryRepository deliveries;

    @Override
    public DashboardView get() {
        List<InventoryItem> items = inventory.findAll();
        long lowStock = items.stream().filter(InventoryItem::isLowStock).count();
        List<WorkOrder> overdue = workOrders.findByExpectedCompletionAtBeforeAndStatusNotIn(Instant.now(), List.of(WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.CANCELLED));
        List<AlertView> alerts = new ArrayList<>();
        overdue.stream().limit(10).forEach(w -> alerts.add(new AlertView("HIGH", "OVERDUE_WORK", w.getOrderNumber(), "Expected completion has passed for " + w.getAsset().getAssetCode())));
        items.stream().filter(InventoryItem::isLowStock).limit(10).forEach(i -> alerts.add(new AlertView("MEDIUM", "LOW_STOCK", i.getSku(), i.getName() + " is at or below reorder level")));
        deliveries.findAll().stream().filter(d -> d.getExpectedDeliveryAt() != null && d.getExpectedDeliveryAt().isBefore(Instant.now())
            && !List.of(Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.INSTALLED, Delivery.DeliveryStatus.CANCELLED).contains(d.getStatus())).limit(10)
            .forEach(d -> alerts.add(new AlertView("HIGH", "DELIVERY_DELAY", d.getDeliveryNumber(), "Delivery ETA has passed for " + d.getAsset().getAssetCode())));
        long active = workOrders.findAll().stream().filter(w -> !List.of(WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.CANCELLED).contains(w.getStatus())).count();
        return new DashboardView(customers.count(), assets.count(), assets.countByType(Asset.AssetType.CONTAINER), assets.countByType(Asset.AssetType.PORTA_CABIN),
            workOrders.count(), active, workOrders.countByStatus(WorkOrder.WorkStatus.READY_FOR_DISPATCH), deliveries.countByStatus(Delivery.DeliveryStatus.IN_TRANSIT),
            deliveries.countByStatus(Delivery.DeliveryStatus.DELAYED), lowStock, alerts);
    }
}
