package com.az.payment.response;

public record ServiceResponse(
        long id,
        String name,
        String description,
        boolean isPayment,
        String imageUrl
) {
}
