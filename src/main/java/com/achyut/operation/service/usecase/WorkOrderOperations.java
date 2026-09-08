package com.achyut.operation.service.usecase;

import com.achyut.operation.api.ApiModels.*;
import java.util.List;

public interface WorkOrderOperations {
    WorkOrderView create(WorkOrderRequest request);
    List<WorkOrderView> list();
    WorkOrderView update(Long id, WorkOrderStatusRequest request);
    WorkUpdateView addUpdate(Long id, WorkUpdateRequest request);
    List<WorkUpdateView> updates(Long id);
}
