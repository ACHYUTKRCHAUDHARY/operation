package com.achyut.operation.search;

import com.achyut.operation.asset.*;
import com.achyut.operation.customer.*;
import com.achyut.operation.delivery.*;
import com.achyut.operation.resilience.FaultToleranceExecutor;
import com.achyut.operation.work.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.elasticsearch-enabled", havingValue = "true")
public class SearchIndexer {
    private final ElasticsearchOperations elasticsearch;
    private final CustomerRepository customers;
    private final AssetRepository assets;
    private final WorkOrderRepository workOrders;
    private final DeliveryRepository deliveries;
    private final FaultToleranceExecutor faultTolerance;

    @Async("searchIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(readOnly = true)
    public void onIndexRequested(SearchIndexRequested event) {
        faultTolerance.run("elasticsearch-index", () -> index(event.type(), event.entityId()));
    }

    @Transactional(readOnly = true)
    public void reindexAll() {
        ensureIndex();
        customers.findAll().forEach(c -> elasticsearch.save(customer(c)));
        assets.findAll().forEach(a -> elasticsearch.save(asset(a)));
        workOrders.findAll().forEach(w -> elasticsearch.save(workOrder(w)));
        deliveries.findAll().forEach(d -> elasticsearch.save(delivery(d)));
    }

    private void index(SearchEntityType type, Long id) {
        ensureIndex();
        switch (type) {
            case CUSTOMER -> customers.findById(id).ifPresent(c -> elasticsearch.save(customer(c)));
            case ASSET -> assets.findById(id).ifPresent(a -> elasticsearch.save(asset(a)));
            case WORK_ORDER -> workOrders.findById(id).ifPresent(w -> elasticsearch.save(workOrder(w)));
            case DELIVERY -> deliveries.findById(id).ifPresent(d -> elasticsearch.save(delivery(d)));
        }
    }

    private void ensureIndex() {
        var index = elasticsearch.indexOps(SearchDocument.class);
        if (!index.exists()) index.createWithMapping();
    }

    private SearchDocument customer(Customer c) {
        return SearchDocument.builder().id(id(SearchEntityType.CUSTOMER, c.getId())).type("CUSTOMER").entityId(c.getId())
            .reference(String.valueOf(c.getId())).title(c.getName()).subtitle(c.getCompanyName())
            .searchText(join(c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getBillingAddress())).build();
    }

    private SearchDocument asset(Asset a) {
        String customer = a.getCustomer() == null ? null : a.getCustomer().getName();
        return SearchDocument.builder().id(id(SearchEntityType.ASSET, a.getId())).type("ASSET").entityId(a.getId())
            .reference(a.getAssetCode()).title(a.getAssetCode()).subtitle(join(customer, a.getSizeDescription(), a.getCurrentYardLocation()))
            .status(a.getStatus().name()).searchText(join(a.getAssetCode(), a.getSerialNumber(), a.getType().name(), a.getStatus().name(), a.getSizeDescription(), a.getCurrentYardLocation(), customer)).build();
    }

    private SearchDocument workOrder(WorkOrder w) {
        return SearchDocument.builder().id(id(SearchEntityType.WORK_ORDER, w.getId())).type("WORK_ORDER").entityId(w.getId())
            .reference(w.getOrderNumber()).title(w.getOrderNumber()).subtitle(join(w.getAsset().getAssetCode(), w.getCustomer().getName(), w.getAssignedTeam()))
            .status(w.getStatus().name()).searchText(join(w.getOrderNumber(), w.getWorkType().name(), w.getStatus().name(), w.getPriority().name(), w.getScopeOfWork(), w.getAssignedTeam(), w.getBlockedReason(), w.getAsset().getAssetCode(), w.getCustomer().getName())).build();
    }

    private SearchDocument delivery(Delivery d) {
        return SearchDocument.builder().id(id(SearchEntityType.DELIVERY, d.getId())).type("DELIVERY").entityId(d.getId())
            .reference(d.getDeliveryNumber()).title(d.getDeliveryNumber()).subtitle(join(d.getAsset().getAssetCode(), d.getVehicleNumber(), d.getDestinationAddress()))
            .status(d.getStatus().name()).searchText(join(d.getDeliveryNumber(), d.getStatus().name(), d.getDriverName(), d.getDriverPhone(), d.getVehicleNumber(), d.getVehicleType(), d.getDestinationAddress(), d.getAsset().getAssetCode(), d.getWorkOrder().getOrderNumber())).build();
    }

    private static String id(SearchEntityType type, Long id) { return type.name() + ":" + id; }
    private static String join(Object... values) {
        return java.util.Arrays.stream(values).filter(Objects::nonNull).map(Object::toString).filter(s -> !s.isBlank()).reduce((a, b) -> a + " " + b).orElse("");
    }
}
