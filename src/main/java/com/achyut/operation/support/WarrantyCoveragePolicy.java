package com.achyut.operation.support;

import com.achyut.operation.asset.Asset;
import java.time.LocalDate;

public interface WarrantyCoveragePolicy {
    boolean isCovered(Asset asset, LocalDate onDate);
}
