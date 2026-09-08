package com.achyut.operation.search;

public interface SearchIndexPort {
    void refresh(SearchEntityType type, Long entityId);
}
