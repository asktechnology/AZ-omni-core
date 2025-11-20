package com.az.payment.request.payment;

import java.io.Serializable;

public record RequestParameter(
        long id,
        String key,
        String value
) implements Serializable {
}
