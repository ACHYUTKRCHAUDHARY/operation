package com.achyut.operation.api;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.audit.AuditEvent;
import com.achyut.operation.service.OperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OperationsController {
    private final OperationsService service;

    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    CustomerView createCustomer(@Valid @RequestBody CustomerRequest request) { return service.createCustomer(request); }
    @GetMapping("/customers") List<CustomerView> customers() { return service.customers(); }

    @PostMapping("/assets") @ResponseStatus(HttpStatus.CREATED)
    AssetView createAsset(@Valid @RequestBody AssetRequest request) { return service.createAsset(request); }
    @GetMapping("/assets") List<AssetView> assets() { return service.assets(); }

    @PostMapping("/work-orders") @ResponseStatus(HttpStatus.CREATED)
    WorkOrderView createWorkOrder(@Valid @RequestBody WorkOrderRequest request) { return service.createWorkOrder(request); }
    @GetMapping("/work-orders") List<WorkOrderView> workOrders() { return service.workOrders(); }
    @PatchMapping("/work-orders/{id}") WorkOrderView updateWorkOrder(@PathVariable Long id, @Valid @RequestBody WorkOrderStatusRequest request) { return service.updateWorkOrder(id, request); }
    @PostMapping("/work-orders/{id}/updates") @ResponseStatus(HttpStatus.CREATED)
    WorkUpdateView addUpdate(@PathVariable Long id, @Valid @RequestBody WorkUpdateRequest request) { return service.addWorkUpdate(id, request); }
    @GetMapping("/work-orders/{id}/updates") List<WorkUpdateView> workUpdates(@PathVariable Long id) { return service.workUpdates(id); }

    @PostMapping("/inventory") @ResponseStatus(HttpStatus.CREATED)
    InventoryView createInventory(@Valid @RequestBody InventoryRequest request) { return service.createInventory(request); }
    @GetMapping("/inventory") List<InventoryView> inventory() { return service.inventory(); }
    @PatchMapping("/inventory/{id}/adjust") InventoryView adjustInventory(@PathVariable Long id, @Valid @RequestBody InventoryAdjustmentRequest request) { return service.adjustInventory(id, request); }

    @PostMapping("/deliveries") @ResponseStatus(HttpStatus.CREATED)
    DeliveryView createDelivery(@Valid @RequestBody DeliveryRequest request) { return service.createDelivery(request); }
    @GetMapping("/deliveries") List<DeliveryView> deliveries() { return service.deliveries(); }
    @PatchMapping("/deliveries/{id}") DeliveryView updateDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryStatusRequest request) { return service.updateDelivery(id, request); }

    @GetMapping("/dashboard") DashboardView dashboard() { return service.dashboard(); }
    @GetMapping("/timeline/{type}/{id}") List<AuditEvent> timeline(@PathVariable String type, @PathVariable Long id) { return service.timeline(type, id); }
}
