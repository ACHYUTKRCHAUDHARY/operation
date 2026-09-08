package com.achyut.operation.work;

import com.achyut.operation.asset.Asset;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkOrderAssetStatusPolicy implements WorkOrderAssetStatusPolicy {
    @Override
    public void synchronize(WorkOrder w) {
        Asset a = w.getAsset();
        switch (w.getStatus()) {
            case INSPECTION_PENDING, ESTIMATE_PENDING, CUSTOMER_APPROVAL_PENDING -> a.setStatus(Asset.AssetStatus.INSPECTION);
            case IN_PROGRESS -> a.setStatus(w.getWorkType() == WorkOrder.WorkType.PORTA_CABIN_PRODUCTION ? Asset.AssetStatus.PRODUCTION_IN_PROGRESS : Asset.AssetStatus.REPAIR_IN_PROGRESS);
            case QUALITY_CHECK -> a.setStatus(Asset.AssetStatus.QUALITY_CHECK);
            case READY_FOR_DISPATCH, COMPLETED -> a.setStatus(Asset.AssetStatus.READY_FOR_DISPATCH);
            case BLOCKED -> a.setStatus(Asset.AssetStatus.ON_HOLD);
            default -> { }
        }
    }
}
