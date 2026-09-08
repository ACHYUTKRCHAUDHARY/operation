package com.achyut.operation.api;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.asset.Asset;
import com.achyut.operation.customer.Customer;
import com.achyut.operation.delivery.Delivery;
import com.achyut.operation.inventory.InventoryItem;
import com.achyut.operation.work.WorkOrder;
import com.achyut.operation.work.WorkUpdate;
import org.springframework.stereotype.Component;

@Component
public class OperationsMapper {
    public CustomerView customer(Customer c) {
        return new CustomerView(c.getId(), c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getBillingAddress());
    }

    public AssetView asset(Asset a) {
        return new AssetView(a.getId(), a.getAssetCode(), a.getType(), a.getStatus(), a.getSizeDescription(), a.getCurrentYardLocation(), a.getCustomer().getId(), a.getCustomer().getName());
    }

    public WorkOrderView workOrder(WorkOrder w) {
        return new WorkOrderView(w.getId(), w.getOrderNumber(), w.getCustomer().getName(), w.getAsset().getAssetCode(), w.getWorkType(), w.getStatus(), w.getPriority(), w.getAssignedTeam(), w.getBlockedReason(), w.getProgressPercent() == null ? 0 : w.getProgressPercent(), w.getEstimatedCost(), w.getApprovedCost(), w.getExpectedCompletionAt(), w.getActualCompletionAt());
    }

    public WorkUpdateView workUpdate(WorkUpdate u) {
        return new WorkUpdateView(u.getId(), u.getStage(), u.getStatus(), u.getAssignedTo(), u.getNote(), u.getPhotoUrl(), u.getCreatedAt());
    }

    public InventoryView inventory(InventoryItem i) {
        return new InventoryView(i.getId(), i.getSku(), i.getName(), i.getUnit(), i.getQuantityOnHand(), i.getReorderLevel(), i.getUnitCost(), i.getPreferredSupplier(), i.isLowStock());
    }

    public DeliveryView delivery(Delivery d) {
        return new DeliveryView(d.getId(), d.getDeliveryNumber(), d.getWorkOrder().getOrderNumber(), d.getAsset().getAssetCode(), d.getStatus(), d.getDriverName(), d.getDriverPhone(), d.getVehicleNumber(), d.getDestinationLatitude(), d.getDestinationLongitude(), d.getDestinationAddress(), d.getExpectedDeliveryAt(), d.getDispatchedAt(), d.getDeliveredAt(), d.getProofOfDeliveryUrl(), d.getReceivedBy());
    }
}
