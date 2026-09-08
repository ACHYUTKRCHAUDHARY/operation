package com.achyut.operation.search;

import com.achyut.operation.asset.*;
import com.achyut.operation.customer.*;
import com.achyut.operation.delivery.*;
import com.achyut.operation.work.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatabaseSearchQueryService {
    private final CustomerRepository customers;
    private final AssetRepository assets;
    private final WorkOrderRepository workOrders;
    private final DeliveryRepository deliveries;

    public List<SearchQueryService.SearchResult> search(String query, SearchEntityType type, String status, int limit) {
        String needle = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        int max = Math.max(1, Math.min(limit, 100));
        Stream<SearchQueryService.SearchResult> stream = Stream.of(customerResults(needle), assetResults(needle), workOrderResults(needle), deliveryResults(needle)).flatMap(Collection::stream);
        if (type != null) stream = stream.filter(r -> r.type().equals(type.name()));
        if (status != null && !status.isBlank()) stream = stream.filter(r -> status.equalsIgnoreCase(r.status()));
        return stream.limit(max).toList();
    }

    private List<SearchQueryService.SearchResult> customerResults(String q) {
        return customers.findAll().stream().filter(c -> contains(q, c.getName(), c.getCompanyName(), c.getPhone(), c.getEmail(), c.getBillingAddress()))
            .map(c -> new SearchQueryService.SearchResult("CUSTOMER", c.getId(), String.valueOf(c.getId()), c.getName(), c.getCompanyName(), null, 1.0)).toList();
    }

    private List<SearchQueryService.SearchResult> assetResults(String q) {
        return assets.findAll().stream().filter(a -> contains(q, a.getAssetCode(), a.getSerialNumber(), a.getSizeDescription(), a.getCurrentYardLocation(), a.getCustomer() == null ? null : a.getCustomer().getName()))
            .map(a -> new SearchQueryService.SearchResult("ASSET", a.getId(), a.getAssetCode(), a.getAssetCode(), a.getSizeDescription(), a.getStatus().name(), 1.0)).toList();
    }

    private List<SearchQueryService.SearchResult> workOrderResults(String q) {
        return workOrders.findAll().stream().filter(w -> contains(q, w.getOrderNumber(), w.getScopeOfWork(), w.getAssignedTeam(), w.getBlockedReason(), w.getCustomer().getName(), w.getAsset().getAssetCode()))
            .map(w -> new SearchQueryService.SearchResult("WORK_ORDER", w.getId(), w.getOrderNumber(), w.getOrderNumber(), w.getAsset().getAssetCode() + " · " + w.getCustomer().getName(), w.getStatus().name(), 1.0)).toList();
    }

    private List<SearchQueryService.SearchResult> deliveryResults(String q) {
        return deliveries.findAll().stream().filter(d -> contains(q, d.getDeliveryNumber(), d.getDriverName(), d.getDriverPhone(), d.getVehicleNumber(), d.getVehicleType(), d.getDestinationAddress(), d.getAsset().getAssetCode(), d.getWorkOrder().getOrderNumber()))
            .map(d -> new SearchQueryService.SearchResult("DELIVERY", d.getId(), d.getDeliveryNumber(), d.getDeliveryNumber(), d.getAsset().getAssetCode() + " · " + value(d.getDestinationAddress()), d.getStatus().name(), 1.0)).toList();
    }

    private static boolean contains(String q, String... values) {
        if (q.isBlank()) return true;
        return Arrays.stream(values).filter(Objects::nonNull).map(v -> v.toLowerCase(Locale.ROOT)).anyMatch(v -> v.contains(q));
    }

    private static String value(String v) { return v == null ? "" : v; }
}
