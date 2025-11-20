package com.az.payment.response;

import java.io.Serializable;

public record BillerResponse(
        long id,
        String name,
        String description,
        String imageUrl
) implements Serializable {
}
