package com.az.payment.request;

import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        long id,
        @NotNull(message = "Category name is required")
        String name,
        @NotNull(message = "Category Description is required")
        String description,
        @NotNull(message = "Image Url is required")
        String imageUrl
) {
}
