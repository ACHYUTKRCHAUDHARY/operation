package com.achyut.operation.support;

import com.achyut.operation.asset.Asset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ActiveWarrantyCoveragePolicy implements WarrantyCoveragePolicy {
    private final WarrantyRepository warranties;

    @Override
    public boolean isCovered(Asset asset, LocalDate onDate) {
        return warranties.findTopByAssetIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(
            asset.getId(), onDate, onDate, Warranty.Status.ACTIVE).isPresent();
    }
}
