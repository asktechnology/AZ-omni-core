package com.az.payment.mapper;

import com.az.payment.domain.Parameter;
import com.az.payment.request.parameter.CreateParameterRequest;
import com.az.payment.request.payment.RequestParameter;
import com.az.payment.response.ParameterResponse;
import com.az.payment.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParametersMapper {

    private final OptionMapper mapper;

    public ParameterResponse toParameterResponse(Parameter parameter) {
        return new ParameterResponse(
                parameter.getId(),
                parameter.getName(),
                parameter.getDescription(),
                parameter.getInternalKey(),
                parameter.getOrderNo(),
                parameter.getRegex(),
                parameter.getInputType(),
                parameter.getJsonType(),
                parameter.getParameterType().ordinal(),
                parameter.getLength(),
                parameter.getMinValue(),
                parameter.getMaxValue(),
                mapper.toOptionsResponse(parameter.getOptions())


        );
    }

    public Parameter toParameter(CreateParameterRequest request) {
        return Parameter.builder()
                .name(request.name())
                .description(request.description())
                .internalKey(request.internalKey())
                .externalKey(request.externalKey())
                .paramType(request.paramType())
                .jsonType(request.jsonType())
                .isFixed(request.isFixed() ? 1 : 0)
                .fixedValue(request.fixedValue())
                .length(request.length())
                .minValue(request.minValue())
                .maxValue(request.maxValue())
                .inputType(request.inputType())
                .isGeneratedTransactionId(request.isGeneratedTransactionId())
                .regex(request.regex())
                .build();
    }

    public RequestParameter toRequestParameter(Parameter parameter) {
        return new RequestParameter(
                parameter.getId(),
                parameter.getInternalKey(),
                parameter.getFixedValue()
        );
    }

    public RequestParameter toRequestParameter(Parameter parameter, String value) {
        return new RequestParameter(
                parameter.getId(),
                parameter.getInternalKey(),
                value
        );
    }

    //TODO:public PaymentResponse toPaymentResponse()
}
