package com.achyut.operation.service.usecase;

import com.achyut.operation.api.ApiModels.InventoryAdjustmentRequest;
import com.achyut.operation.api.ApiModels.InventoryRequest;
import com.achyut.operation.api.ApiModels.InventoryView;
import java.util.List;

public interface InventoryOperations {
    InventoryView create(InventoryRequest request);
    List<InventoryView> list();
    InventoryView adjust(Long id, InventoryAdjustmentRequest request);
}
