package com.achyut.operation.service.usecase;

import com.achyut.operation.api.ApiModels.DeliveryRequest;
import com.achyut.operation.api.ApiModels.DeliveryStatusRequest;
import com.achyut.operation.api.ApiModels.DeliveryView;
import java.util.List;

public interface DeliveryOperations {
    DeliveryView create(DeliveryRequest request);
    List<DeliveryView> list();
    DeliveryView update(Long id, DeliveryStatusRequest request);
}
