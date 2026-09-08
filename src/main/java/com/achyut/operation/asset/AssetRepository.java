package com.achyut.operation.asset;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByAssetCode(String assetCode);
    long countByType(Asset.AssetType type);
    long countByStatus(Asset.AssetStatus status);

    @Override
    @EntityGraph(attributePaths = {"customer"})
    List<Asset> findAll();
}
