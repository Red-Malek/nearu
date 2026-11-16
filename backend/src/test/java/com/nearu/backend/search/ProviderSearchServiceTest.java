package com.nearu.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderSearchServiceTest {

    private final ProviderSearchRepository repository = mock(ProviderSearchRepository.class);
    private final ProviderSearchService service = new ProviderSearchService(repository);

    @Test
    void passesCategoryAndCityToRepository() {
        var ahmed = new ProviderSearchResult(UUID.randomUUID(), "Ahmed Trabelsi", "Ariana", List.of("Électricité"));
        when(repository.search("electricite", "ariana")).thenReturn(List.of(ahmed));

        assertThat(service.search("electricite", "ariana")).containsExactly(ahmed);
    }

    @Test
    void treatsBlankFiltersAsNoFilter() {
        when(repository.search(null, null)).thenReturn(List.of());

        service.search("  ", "");

        verify(repository).search(null, null);
    }

    @Test
    void returnsEmptyListWhenNobodyMatches() {
        when(repository.search("plomberie", "tozeur")).thenReturn(List.of());

        assertThat(service.search("plomberie", "tozeur")).isEmpty();
    }
}
