package com.nearu.backend.search;

import java.util.List;
import java.util.UUID;

/** One provider in the search results: name, métier(s) and ville (US04 criterion 2). */
public record ProviderSearchResult(UUID id, String name, String city, List<String> services) {
}
