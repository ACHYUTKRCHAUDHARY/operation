package com.achyut.operation.fleet;

public interface AssignmentEligibilityPolicy {
    void validate(Driver driver, Vehicle vehicle);
}
