package com.achyut.operation.delivery;

import com.achyut.operation.asset.Asset;
import org.springframework.stereotype.Component;

@Component
public class DefaultDeliveryAssetStatusPolicy implements DeliveryAssetStatusPolicy {
    @Override
    public void synchronize(Delivery d) {
        Asset a = d.getAsset();
        switch (d.getStatus()) {
            case DISPATCHED -> a.setStatus(Asset.AssetStatus.DISPATCHED);
            case IN_TRANSIT, DELAYED, NEAR_DESTINATION -> a.setStatus(Asset.AssetStatus.IN_TRANSIT);
            case DELIVERED, INSTALLATION_IN_PROGRESS -> a.setStatus(Asset.AssetStatus.DELIVERED);
            case INSTALLED -> a.setStatus(Asset.AssetStatus.INSTALLED);
            default -> { }
        }
    }
}
