package com.achyut.operation.delivery;

import com.achyut.operation.common.TransitionPolicy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class DeliveryTransitionPolicy implements TransitionPolicy<Delivery.DeliveryStatus> {
    private final Map<Delivery.DeliveryStatus, Set<Delivery.DeliveryStatus>> allowed = new EnumMap<>(Delivery.DeliveryStatus.class);

    public DeliveryTransitionPolicy() {
        allow(Delivery.DeliveryStatus.PLANNED, Delivery.DeliveryStatus.VEHICLE_ASSIGNED, Delivery.DeliveryStatus.DISPATCHED, Delivery.DeliveryStatus.CANCELLED);
        allow(Delivery.DeliveryStatus.VEHICLE_ASSIGNED, Delivery.DeliveryStatus.DISPATCHED, Delivery.DeliveryStatus.CANCELLED);
        allow(Delivery.DeliveryStatus.DISPATCHED, Delivery.DeliveryStatus.IN_TRANSIT, Delivery.DeliveryStatus.DELAYED, Delivery.DeliveryStatus.CANCELLED);
        allow(Delivery.DeliveryStatus.IN_TRANSIT, Delivery.DeliveryStatus.NEAR_DESTINATION, Delivery.DeliveryStatus.DELAYED, Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.CANCELLED);
        allow(Delivery.DeliveryStatus.DELAYED, Delivery.DeliveryStatus.IN_TRANSIT, Delivery.DeliveryStatus.NEAR_DESTINATION, Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.CANCELLED);
        allow(Delivery.DeliveryStatus.NEAR_DESTINATION, Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.DELAYED, Delivery.DeliveryStatus.CANCELLED);
        allow(Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.INSTALLATION_IN_PROGRESS, Delivery.DeliveryStatus.INSTALLED);
        allow(Delivery.DeliveryStatus.INSTALLATION_IN_PROGRESS, Delivery.DeliveryStatus.INSTALLED);
        allow(Delivery.DeliveryStatus.INSTALLED);
        allow(Delivery.DeliveryStatus.CANCELLED);
    }

    @Override
    public void validate(Delivery.DeliveryStatus current, Delivery.DeliveryStatus target) {
        if (current == target) return;
        if (!allowed.getOrDefault(current, Set.of()).contains(target)) {
            throw new IllegalStateException("Invalid delivery transition: " + current + " -> " + target);
        }
    }

    private void allow(Delivery.DeliveryStatus from, Delivery.DeliveryStatus... targets) {
        allowed.put(from, targets.length == 0 ? EnumSet.noneOf(Delivery.DeliveryStatus.class) : EnumSet.copyOf(java.util.List.of(targets)));
    }
}
