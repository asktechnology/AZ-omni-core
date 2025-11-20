package com.az.payment.response;

import com.az.payment.domain.ParameterDataType;

import java.util.List;

public record InParameterResponse(
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
        List<OptionResponse> option,
        String externalKey,
        ParameterDataType parameterDataType,
        boolean isResponseParam,
        String nameAr
) {
}
