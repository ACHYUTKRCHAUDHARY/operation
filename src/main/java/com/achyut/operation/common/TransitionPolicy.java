package com.achyut.operation.common;

public interface TransitionPolicy<S> {
    void validate(S current, S target);
}
