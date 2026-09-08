package com.achyut.operation.service.usecase;

import com.achyut.operation.api.ApiModels.AssetRequest;
import com.achyut.operation.api.ApiModels.AssetView;
import java.util.List;

public interface AssetOperations {
    AssetView create(AssetRequest request);
    List<AssetView> list();
}
