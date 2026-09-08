package com.achyut.operation.inventory;

import com.achyut.operation.api.ApiModels.*;
import com.achyut.operation.api.OperationsMapper;
import com.achyut.operation.common.AuditPort;
import com.achyut.operation.service.usecase.InventoryOperations;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService implements InventoryOperations {
    private final InventoryItemRepository repository;
    private final OperationsMapper mapper;
    private final AuditPort audit;

    @Override
    public InventoryView create(InventoryRequest r) {
        InventoryItem item = InventoryItem.builder().sku(r.sku()).name(r.name()).unit(r.unit()).quantityOnHand(r.quantityOnHand())
            .reorderLevel(r.reorderLevel()).unitCost(r.unitCost()).preferredSupplier(r.preferredSupplier()).build();
        return mapper.inventory(repository.save(item));
    }

    @Override
    public List<InventoryView> list() { return repository.findAll().stream().map(mapper::inventory).toList(); }

    @Override
    public InventoryView adjust(Long id, InventoryAdjustmentRequest r) {
        InventoryItem item = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Inventory item not found"));
        BigDecimal current = item.getQuantityOnHand() == null ? BigDecimal.ZERO : item.getQuantityOnHand();
        BigDecimal next = current.add(r.delta());
        if (next.signum() < 0) throw new IllegalArgumentException("Inventory cannot become negative");
        item.setQuantityOnHand(next);
        audit.record("INVENTORY", id, "STOCK_ADJUSTED", r.actor(), r.delta() + " " + item.getUnit() + (r.reason() == null ? "" : " | " + r.reason()));
        return mapper.inventory(item);
    }
}
