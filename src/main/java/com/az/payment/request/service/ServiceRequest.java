package com.az.payment.request.service;

public record ServiceRequest(
        long id,
        String name,
        String description,
        boolean isPayment
) {
}
