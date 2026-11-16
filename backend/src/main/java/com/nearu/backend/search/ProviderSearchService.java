package com.nearu.backend.search;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderSearchService {

    private final ProviderSearchRepository repository;

    public ProviderSearchService(ProviderSearchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProviderSearchResult> search(String category, String city) {
        return repository.search(blankToNull(category), blankToNull(city));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
