package com.achyut.operation.search;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchQueryService search;

    @GetMapping
    public List<SearchQueryService.SearchResult> search(
        @RequestParam(defaultValue = "") String q,
        @RequestParam(required = false) SearchEntityType type,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "25") int limit) {
        return search.search(q, type, status, limit);
    }
}
