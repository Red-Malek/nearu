package com.nearu.backend.search;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderSearchController {

    private final ProviderSearchService service;

    public ProviderSearchController(ProviderSearchService service) {
        this.service = service;
    }

    /** category = category slug, city = governorate code; both optional. */
    @GetMapping
    public List<ProviderSearchResult> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city) {
        return service.search(category, city);
    }
}
