package com.achyut.operation.asset;

import com.achyut.operation.api.ApiModels.AssetRequest;
import com.achyut.operation.api.ApiModels.AssetView;
import com.achyut.operation.api.OperationsMapper;
import com.achyut.operation.common.AuditPort;
import com.achyut.operation.customer.Customer;
import com.achyut.operation.customer.CustomerRepository;
import com.achyut.operation.service.usecase.AssetOperations;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetService implements AssetOperations {
    private final AssetRepository assets;
    private final CustomerRepository customers;
    private final OperationsMapper mapper;
    private final AuditPort audit;

    @Override
    public AssetView create(AssetRequest r) {
        Customer customer = customers.findById(r.customerId()).orElseThrow(() -> new NoSuchElementException("Customer not found"));
        Asset asset = Asset.builder().assetCode(r.assetCode()).type(r.type()).status(Asset.AssetStatus.RECEIVED)
            .sizeDescription(r.sizeDescription()).serialNumber(r.serialNumber()).currentYardLocation(r.currentYardLocation()).customer(customer).build();
        asset = assets.save(asset);
        audit.record("ASSET", asset.getId(), "ASSET_CREATED", "system", asset.getAssetCode() + " created");
        return mapper.asset(asset);
    }

    @Override
    public List<AssetView> list() {
        return assets.findAll().stream().map(mapper::asset).toList();
    }
}
