package com.achyut.operation.delivery;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.api.OperationsMapper;
import com.achyut.operation.asset.*;
import com.achyut.operation.common.*;
import com.achyut.operation.service.usecase.DeliveryOperations;
import com.achyut.operation.work.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryService implements DeliveryOperations {
    private final DeliveryRepository deliveries;
    private final WorkOrderRepository workOrders;
    private final AssetRepository assets;
    private final OperationsMapper mapper;
    private final ReferenceNumberGenerator numbers;
    private final AuditPort audit;
    private final DeliveryAssetStatusPolicy assetStatusPolicy;

    @Override
    public DeliveryView create(DeliveryRequest r) {
        WorkOrder workOrder = workOrders.findById(r.workOrderId()).orElseThrow(() -> new NoSuchElementException("Work order not found"));
        Asset asset = assets.findById(r.assetId()).orElseThrow(() -> new NoSuchElementException("Asset not found"));
        if (!Objects.equals(workOrder.getAsset().getId(), asset.getId())) throw new IllegalArgumentException("Work order and asset do not match");
        Delivery delivery = Delivery.builder().deliveryNumber(numbers.next("DEL")).workOrder(workOrder).asset(asset).status(Delivery.DeliveryStatus.PLANNED)
            .driverName(r.driverName()).driverPhone(r.driverPhone()).vehicleNumber(r.vehicleNumber()).vehicleType(r.vehicleType())
            .destinationLatitude(r.destinationLatitude()).destinationLongitude(r.destinationLongitude()).destinationAddress(r.destinationAddress())
            .expectedDeliveryAt(r.expectedDeliveryAt()).build();
        delivery = deliveries.save(delivery);
        audit.record("DELIVERY", delivery.getId(), "DELIVERY_CREATED", "system", delivery.getDeliveryNumber() + " for " + asset.getAssetCode());
        return mapper.delivery(delivery);
    }

    @Override
    public List<DeliveryView> list() { return deliveries.findAll().stream().map(mapper::delivery).toList(); }

    @Override
    public DeliveryView update(Long id, DeliveryStatusRequest r) {
        Delivery delivery = deliveries.findById(id).orElseThrow(() -> new NoSuchElementException("Delivery not found: " + id));
        Delivery.DeliveryStatus old = delivery.getStatus();
        delivery.setStatus(r.status());
        if (r.status() == Delivery.DeliveryStatus.DISPATCHED && delivery.getDispatchedAt() == null) delivery.setDispatchedAt(Instant.now());
        if (r.status() == Delivery.DeliveryStatus.DELIVERED || r.status() == Delivery.DeliveryStatus.INSTALLED) {
            if (delivery.getDeliveredAt() == null) delivery.setDeliveredAt(Instant.now());
            if (r.proofOfDeliveryUrl() != null) delivery.setProofOfDeliveryUrl(r.proofOfDeliveryUrl());
            if (r.receivedBy() != null) delivery.setReceivedBy(r.receivedBy());
        }
        assetStatusPolicy.synchronize(delivery);
        audit.record("DELIVERY", id, "STATUS_CHANGED", r.actor(), old + " -> " + r.status());
        return mapper.delivery(delivery);
    }
}
