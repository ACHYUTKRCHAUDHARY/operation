package com.achyut.operation.search;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.elasticsearch-enabled", havingValue = "true")
public class ElasticsearchSearchEngine {
    private final ElasticsearchOperations operations;

    public List<SearchQueryService.SearchResult> search(String query, SearchEntityType type, String status, int limit) {
        Criteria criteria = new Criteria("searchText").matches(query == null || query.isBlank() ? "*" : query);
        if (type != null) criteria = criteria.and("type").is(type.name());
        if (status != null && !status.isBlank()) criteria = criteria.and("status").is(status.toUpperCase());
        CriteriaQuery elasticQuery = new CriteriaQuery(criteria);
        elasticQuery.setPageable(PageRequest.of(0, Math.max(1, Math.min(limit, 100))));
        return operations.search(elasticQuery, SearchDocument.class).stream().map(this::result).toList();
    }

    private SearchQueryService.SearchResult result(SearchHit<SearchDocument> hit) {
        SearchDocument d = hit.getContent();
        return new SearchQueryService.SearchResult(d.getType(), d.getEntityId(), d.getReference(), d.getTitle(), d.getSubtitle(), d.getStatus(), hit.getScore());
    }
}
