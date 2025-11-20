package com.az.payment.response;

import java.io.Serializable;
import java.util.List;

public record ParameterResponse(
        long id,
        String name,
        String description,
        String internalKey,
        int orderNo,
        String regex,
        int inputType,
        int jsonType,
        int paramType,
        int length,
        double minValue,
        double maxValue,
        List<OptionResponse> option
) implements Serializable {
}
