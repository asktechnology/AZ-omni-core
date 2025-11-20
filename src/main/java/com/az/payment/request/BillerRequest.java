package com.az.payment.request;

public record BillerRequest(
        long id,
        String name,
        String description,
        String baseUrl,
        String imageUrl
) {
}
