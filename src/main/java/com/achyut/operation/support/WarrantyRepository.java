package com.achyut.operation.support;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface WarrantyRepository extends JpaRepository<Warranty, Long> {
    Optional<Warranty> findTopByAssetIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatus(Long assetId, LocalDate start, LocalDate end, Warranty.Status status);
}
