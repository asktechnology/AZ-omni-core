package com.az.payment.request.parameter;

public record CreateParameterRequest(
        String name,
        String description,
        String internalKey,
        String externalKey,
        int inputType,
        boolean isFixed,
        String fixedValue,
        boolean isGeneratedTransactionId,
        int jsonType,
        int length,
        double minValue,
        double maxValue,
        int paramType,
        String regex

) {
}
