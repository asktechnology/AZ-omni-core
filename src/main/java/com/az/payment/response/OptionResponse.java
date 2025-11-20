package com.az.payment.response;

import java.io.Serializable;

public record OptionResponse(
        long id,
        String name,
        String value
) implements Serializable {
}
