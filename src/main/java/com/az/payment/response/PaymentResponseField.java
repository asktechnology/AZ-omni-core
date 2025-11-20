package com.az.payment.response;

import java.io.Serializable;

public record PaymentResponseField(
        String key,
        Object value,
        String displayName,
        long id
) implements Serializable {


}
