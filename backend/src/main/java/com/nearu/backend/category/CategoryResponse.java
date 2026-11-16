package com.nearu.backend.category;

import java.util.UUID;

public record CategoryResponse(UUID id, String slug, String name, String icon) {

    static CategoryResponse from(ServiceCategory c) {
        return new CategoryResponse(c.getId(), c.getSlug(), c.getName(), c.getIcon());
    }
}
