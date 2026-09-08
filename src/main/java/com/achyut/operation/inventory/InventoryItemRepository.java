package com.achyut.operation.inventory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);

    @Query("select count(i) from InventoryItem i where i.quantityOnHand <= i.reorderLevel")
    long countLowStock();

    @Query("select i from InventoryItem i where i.quantityOnHand <= i.reorderLevel order by i.quantityOnHand asc")
    List<InventoryItem> findLowStock(Pageable pageable);
}
