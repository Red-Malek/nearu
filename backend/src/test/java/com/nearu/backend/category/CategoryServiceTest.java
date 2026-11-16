package com.nearu.backend.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

    private final ServiceCategoryRepository repository = mock(ServiceCategoryRepository.class);
    private final CategoryService service = new CategoryService(repository);

    @Test
    void returnsActiveCategoriesInRepositoryOrder() {
        when(repository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(
                new ServiceCategory(UUID.randomUUID(), "plomberie", "Plomberie", "plumbing", 1, true),
                new ServiceCategory(UUID.randomUUID(), "electricite", "Électricité", "electricity", 2, true)));

        List<CategoryResponse> result = service.listActiveCategories();

        assertThat(result).extracting(CategoryResponse::slug)
                .containsExactly("plomberie", "electricite");
        assertThat(result.get(0).name()).isEqualTo("Plomberie");
    }

    @Test
    void returnsEmptyListWhenNoCategory() {
        when(repository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());

        assertThat(service.listActiveCategories()).isEmpty();
    }
}
