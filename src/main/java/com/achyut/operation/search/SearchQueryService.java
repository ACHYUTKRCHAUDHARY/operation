package com.achyut.operation.search;

import java.util.List;

public interface SearchQueryService {
    List<SearchResult> search(String query, SearchEntityType type, String status, int limit);

    record SearchResult(String type, Long entityId, String reference, String title, String subtitle, String status, double score) {}
}
