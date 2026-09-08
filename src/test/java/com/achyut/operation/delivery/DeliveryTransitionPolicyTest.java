package com.achyut.operation.delivery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryTransitionPolicyTest {
    private final DeliveryTransitionPolicy policy = new DeliveryTransitionPolicy();

    @Test
    void allowsNormalDeliveryLifecycle() {
        assertDoesNotThrow(() -> policy.validate(Delivery.DeliveryStatus.PLANNED, Delivery.DeliveryStatus.VEHICLE_ASSIGNED));
        assertDoesNotThrow(() -> policy.validate(Delivery.DeliveryStatus.IN_TRANSIT, Delivery.DeliveryStatus.NEAR_DESTINATION));
        assertDoesNotThrow(() -> policy.validate(Delivery.DeliveryStatus.DELIVERED, Delivery.DeliveryStatus.INSTALLATION_IN_PROGRESS));
    }

    @Test
    void rejectsDeliverySkippingToDelivered() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> policy.validate(Delivery.DeliveryStatus.PLANNED, Delivery.DeliveryStatus.DELIVERED));
        assertTrue(ex.getMessage().contains("Invalid delivery transition"));
    }
}
