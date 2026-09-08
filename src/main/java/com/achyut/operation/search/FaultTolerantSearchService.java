package com.achyut.operation.search;

import com.achyut.operation.resilience.FaultToleranceExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaultTolerantSearchService implements SearchQueryService {
    private final ObjectProvider<ElasticsearchSearchEngine> elasticsearch;
    private final DatabaseSearchQueryService databaseFallback;
    private final FaultToleranceExecutor faultTolerance;

    @Override
    public List<SearchResult> search(String query, SearchEntityType type, String status, int limit) {
        ElasticsearchSearchEngine engine = elasticsearch.getIfAvailable();
        if (engine == null) return databaseFallback.search(query, type, status, limit);
        return faultTolerance.execute(
            "elasticsearch-search",
            () -> engine.search(query, type, status, limit),
            () -> databaseFallback.search(query, type, status, limit)
        );
    }
}
