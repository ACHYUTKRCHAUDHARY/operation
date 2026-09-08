package com.achyut.operation.service;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.asset.*;
import com.achyut.operation.delivery.*;
import com.achyut.operation.inventory.*;
import com.achyut.operation.service.usecase.DashboardQuery;
import com.achyut.operation.work.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardQueryService implements DashboardQuery {
    private static final List<WorkOrder.WorkStatus> CLOSED_WORK_STATUSES =
        List.of(WorkOrder.WorkStatus.COMPLETED, WorkOrder.WorkStatus.CANCELLED);
    private static final List<Delivery.DeliveryStatus> CLOSED_DELIVERY_STATUSES =
        List.of(Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.INSTALLED, Delivery.DeliveryStatus.CANCELLED);
    private static final PageRequest ALERT_LIMIT = PageRequest.of(0, 10);

    private final com.achyut.operation.customer.CustomerRepository customers;
    private final AssetRepository assets;
    private final WorkOrderRepository workOrders;
    private final InventoryItemRepository inventory;
    private final DeliveryRepository deliveries;

    @Override
    public DashboardView get() {
        Instant now = Instant.now();
        long lowStock = inventory.countLowStock();
        long active = workOrders.countByStatusNotIn(CLOSED_WORK_STATUSES);

        List<AlertView> alerts = new ArrayList<>(30);
        workOrders.findOverdueForDashboard(now, CLOSED_WORK_STATUSES, ALERT_LIMIT)
            .forEach(w -> alerts.add(new AlertView("HIGH", "OVERDUE_WORK", w.getOrderNumber(),
                "Expected completion has passed for " + w.getAsset().getAssetCode())));

        inventory.findLowStock(ALERT_LIMIT)
            .forEach(i -> alerts.add(new AlertView("MEDIUM", "LOW_STOCK", i.getSku(),
                i.getName() + " is at or below reorder level")));

        deliveries.findOverdueForDashboard(now, CLOSED_DELIVERY_STATUSES, ALERT_LIMIT)
            .forEach(d -> alerts.add(new AlertView("HIGH", "DELIVERY_DELAY", d.getDeliveryNumber(),
                "Delivery ETA has passed for " + d.getAsset().getAssetCode())));

        return new DashboardView(
            customers.count(),
            assets.count(),
            assets.countByType(Asset.AssetType.CONTAINER),
            assets.countByType(Asset.AssetType.PORTA_CABIN),
            workOrders.count(),
            active,
            workOrders.countByStatus(WorkOrder.WorkStatus.READY_FOR_DISPATCH),
            deliveries.countByStatus(Delivery.DeliveryStatus.IN_TRANSIT),
            deliveries.countByStatus(Delivery.DeliveryStatus.DELAYED),
            lowStock,
            alerts
        );
    }
}
