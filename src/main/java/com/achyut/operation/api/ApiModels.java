package com.achyut.operation.api;

import com.achyut.operation.asset.Asset;
import com.achyut.operation.delivery.Delivery;
import com.achyut.operation.work.WorkOrder;
import com.achyut.operation.work.WorkUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() {}

    public record CustomerRequest(@NotBlank String name, String companyName, String phone, @Email String email, String billingAddress) {}
    public record CustomerView(Long id, String name, String companyName, String phone, String email, String billingAddress) {}

    public record AssetRequest(@NotBlank String assetCode, @NotNull Asset.AssetType type, String sizeDescription, String serialNumber, String currentYardLocation, @NotNull Long customerId) {}
    public record AssetView(Long id, String assetCode, Asset.AssetType type, Asset.AssetStatus status, String sizeDescription, String currentYardLocation, Long customerId, String customerName) {}

    public record WorkOrderRequest(@NotNull Long customerId, @NotNull Long assetId, @NotNull WorkOrder.WorkType workType, WorkOrder.Priority priority, String scopeOfWork, String assignedTeam, BigDecimal estimatedCost, Instant expectedCompletionAt) {}
    public record WorkOrderStatusRequest(@NotNull WorkOrder.WorkStatus status, Integer progressPercent, String blockedReason, BigDecimal approvedCost, String actor) {}
    public record WorkOrderView(Long id, String orderNumber, String customerName, String assetCode, WorkOrder.WorkType workType, WorkOrder.WorkStatus status, WorkOrder.Priority priority, String assignedTeam, String blockedReason, int progressPercent, BigDecimal estimatedCost, BigDecimal approvedCost, Instant expectedCompletionAt, Instant actualCompletionAt) {}

    public record WorkUpdateRequest(@NotNull WorkUpdate.Stage stage, @NotNull WorkUpdate.StageStatus status, String assignedTo, String note, String photoUrl, String actor) {}
    public record WorkUpdateView(Long id, WorkUpdate.Stage stage, WorkUpdate.StageStatus status, String assignedTo, String note, String photoUrl, Instant createdAt) {}

    public record InventoryRequest(@NotBlank String sku, @NotBlank String name, String unit, @NotNull @PositiveOrZero BigDecimal quantityOnHand, @NotNull @PositiveOrZero BigDecimal reorderLevel, @PositiveOrZero BigDecimal unitCost, String preferredSupplier) {}
    public record InventoryAdjustmentRequest(@NotNull BigDecimal delta, String actor, String reason) {}
    public record InventoryView(Long id, String sku, String name, String unit, BigDecimal quantityOnHand, BigDecimal reorderLevel, BigDecimal unitCost, String preferredSupplier, boolean lowStock) {}

    public record DeliveryRequest(@NotNull Long workOrderId, @NotNull Long assetId, String driverName, String driverPhone, String vehicleNumber, String vehicleType, @NotNull Double destinationLatitude, @NotNull Double destinationLongitude, @NotBlank String destinationAddress, Instant expectedDeliveryAt) {}
    public record DeliveryStatusRequest(@NotNull Delivery.DeliveryStatus status, String proofOfDeliveryUrl, String receivedBy, String actor) {}
    public record DeliveryView(Long id, String deliveryNumber, String orderNumber, String assetCode, Delivery.DeliveryStatus status, String driverName, String driverPhone, String vehicleNumber, Double destinationLatitude, Double destinationLongitude, String destinationAddress, Instant expectedDeliveryAt, Instant dispatchedAt, Instant deliveredAt, String proofOfDeliveryUrl, String receivedBy) {}

    public record LocationRequest(@NotNull Double latitude, @NotNull Double longitude, Double accuracyMeters, Double speedKph) {}
    public record LocationView(Long id, Long deliveryId, Double latitude, Double longitude, Double accuracyMeters, Double speedKph, double distanceToDestinationMeters, Instant recordedAt) {}

    public record AlertView(String severity, String type, String reference, String message) {}
    public record DashboardView(long customers, long assets, long containers, long portaCabins, long workOrders, long activeWorkOrders, long readyForDispatch, long deliveriesInTransit, long delayedDeliveries, long lowStockItems, List<AlertView> alerts) {}
}
