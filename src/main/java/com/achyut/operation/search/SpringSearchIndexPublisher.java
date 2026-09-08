package com.achyut.operation.search;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringSearchIndexPublisher implements SearchIndexPort {
    private final ApplicationEventPublisher events;

    @Override
    public void refresh(SearchEntityType type, Long entityId) {
        if (entityId != null) events.publishEvent(new SearchIndexRequested(type, entityId));
    }
}
