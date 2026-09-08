package com.achyut.operation.search;

import com.achyut.operation.resilience.FaultToleranceExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.search.elasticsearch-enabled", havingValue = "true")
public class SearchBootstrap implements ApplicationRunner {
    private final SearchIndexer indexer;
    private final FaultToleranceExecutor faultTolerance;

    @Override
    public void run(ApplicationArguments args) {
        faultTolerance.run("elasticsearch-index", indexer::reindexAll);
    }
}
