package com.achyut.operation.api;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.audit.AuditEvent;
import com.achyut.operation.service.usecase.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OperationsController {
    private final CustomerOperations customers;
    private final AssetOperations assets;
    private final WorkOrderOperations workOrders;
    private final InventoryOperations inventory;
    private final DeliveryOperations deliveries;
    private final DashboardQuery dashboard;
    private final AuditQuery audit;

    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    CustomerView createCustomer(@Valid @RequestBody CustomerRequest request) { return customers.create(request); }
    @GetMapping("/customers") List<CustomerView> customers() { return customers.list(); }

    @PostMapping("/assets") @ResponseStatus(HttpStatus.CREATED)
    AssetView createAsset(@Valid @RequestBody AssetRequest request) { return assets.create(request); }
    @GetMapping("/assets") List<AssetView> assets() { return assets.list(); }

    @PostMapping("/work-orders") @ResponseStatus(HttpStatus.CREATED)
    WorkOrderView createWorkOrder(@Valid @RequestBody WorkOrderRequest request) { return workOrders.create(request); }
    @GetMapping("/work-orders") List<WorkOrderView> workOrders() { return workOrders.list(); }
    @PatchMapping("/work-orders/{id}") WorkOrderView updateWorkOrder(@PathVariable Long id, @Valid @RequestBody WorkOrderStatusRequest request) { return workOrders.update(id, request); }
    @PostMapping("/work-orders/{id}/updates") @ResponseStatus(HttpStatus.CREATED)
    WorkUpdateView addUpdate(@PathVariable Long id, @Valid @RequestBody WorkUpdateRequest request) { return workOrders.addUpdate(id, request); }
    @GetMapping("/work-orders/{id}/updates") List<WorkUpdateView> workUpdates(@PathVariable Long id) { return workOrders.updates(id); }

    @PostMapping("/inventory") @ResponseStatus(HttpStatus.CREATED)
    InventoryView createInventory(@Valid @RequestBody InventoryRequest request) { return inventory.create(request); }
    @GetMapping("/inventory") List<InventoryView> inventory() { return inventory.list(); }
    @PatchMapping("/inventory/{id}/adjust") InventoryView adjustInventory(@PathVariable Long id, @Valid @RequestBody InventoryAdjustmentRequest request) { return inventory.adjust(id, request); }

    @PostMapping("/deliveries") @ResponseStatus(HttpStatus.CREATED)
    DeliveryView createDelivery(@Valid @RequestBody DeliveryRequest request) { return deliveries.create(request); }
    @GetMapping("/deliveries") List<DeliveryView> deliveries() { return deliveries.list(); }
    @PatchMapping("/deliveries/{id}") DeliveryView updateDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryStatusRequest request) { return deliveries.update(id, request); }

    @GetMapping("/dashboard") DashboardView dashboard() { return dashboard.get(); }
    @GetMapping("/timeline/{type}/{id}") List<AuditEvent> timeline(@PathVariable String type, @PathVariable Long id) { return audit.timeline(type, id); }
}
