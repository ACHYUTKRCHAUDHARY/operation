package com.achyut.operation.config;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.asset.Asset;
import com.achyut.operation.customer.CustomerRepository;
import com.achyut.operation.delivery.Delivery;
import com.achyut.operation.service.OperationsService;
import com.achyut.operation.work.WorkOrder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Profile("!prod")
public class DemoDataConfig implements ApplicationRunner {
    private final OperationsService service;
    private final CustomerRepository customerRepository;

    public DemoDataConfig(OperationsService service, CustomerRepository customerRepository) {
        this.service = service;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customerRepository.count() > 0) return;

        CustomerView customer = service.createCustomer(new CustomerRequest("Rajiv Sharma", "Northline Infrastructure", "+91 9876543210", "operations@northline.example", "Sector 63, Noida, Uttar Pradesh"));

        AssetView container = service.createAsset(new AssetRequest("CONT-1024", Asset.AssetType.CONTAINER, "20 ft dry container", "MSCU-1024", "Repair Bay 2", customer.id()));
        AssetView cabin = service.createAsset(new AssetRequest("PC-204", Asset.AssetType.PORTA_CABIN, "20 x 10 ft", "PC-SN-204", "Fabrication Bay 1", customer.id()));

        WorkOrderView repair = service.createWorkOrder(new WorkOrderRequest(customer.id(), container.id(), WorkOrder.WorkType.CONTAINER_REPAIR, WorkOrder.Priority.HIGH, "Door alignment, floor patching, rust treatment and painting", "Repair Team B", new BigDecimal("28500"), Instant.now().plus(2, ChronoUnit.DAYS)));
        service.updateWorkOrder(repair.id(), new WorkOrderStatusRequest(WorkOrder.WorkStatus.IN_PROGRESS, 65, null, new BigDecimal("28500"), "Workshop Manager"));

        WorkOrderView production = service.createWorkOrder(new WorkOrderRequest(customer.id(), cabin.id(), WorkOrder.WorkType.PORTA_CABIN_PRODUCTION, WorkOrder.Priority.URGENT, "Fabrication, insulation, electrical, interior finish and painting", "Cabin Team A", new BigDecimal("245000"), Instant.now().plus(1, ChronoUnit.DAYS)));
        service.updateWorkOrder(production.id(), new WorkOrderStatusRequest(WorkOrder.WorkStatus.READY_FOR_DISPATCH, 100, null, new BigDecimal("245000"), "Production Manager"));

        service.createInventory(new InventoryRequest("STEEL-1.2MM", "1.2mm Steel Sheet", "sheet", new BigDecimal("18"), new BigDecimal("20"), new BigDecimal("1950"), "Metro Steel Traders"));
        service.createInventory(new InventoryRequest("PAINT-WHITE-20L", "Industrial White Paint 20L", "bucket", new BigDecimal("12"), new BigDecimal("5"), new BigDecimal("4200"), "ColourCoat Supply"));
        service.createInventory(new InventoryRequest("PLY-18MM", "18mm Marine Plywood", "sheet", new BigDecimal("7"), new BigDecimal("10"), new BigDecimal("2850"), "BuildBoard Depot"));

        DeliveryView delivery = service.createDelivery(new DeliveryRequest(production.id(), cabin.id(), "Rakesh Kumar", "+91 9811112233", "UP16-T-4821", "Flatbed Truck", 28.4595, 77.0266, "Sector 39, Gurugram, Haryana", Instant.now().plus(5, ChronoUnit.HOURS)));
        service.updateDelivery(delivery.id(), new DeliveryStatusRequest(Delivery.DeliveryStatus.IN_TRANSIT, null, null, "Dispatch Manager"));
    }
}
