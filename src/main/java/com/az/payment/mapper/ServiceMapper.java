package com.az.payment.mapper;

import com.az.payment.domain.Option;
import com.az.payment.domain.Parameter;
import com.az.payment.request.service.ServiceRequest;
import com.az.payment.response.InParameterResponse;
import com.az.payment.response.OptionResponse;
import com.az.payment.response.ParameterResponse;
import com.az.payment.response.ServiceResponse;
import org.springframework.stereotype.Service;

@Service
public class ServiceMapper {
    public ServiceResponse toServiceResponse(com.az.payment.domain.Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.isPayment(),
                service.getImageUrl()
        );
    }

    public com.az.payment.domain.Service toService(ServiceRequest request) {
        return com.az.payment.domain.Service.
                builder()
                .id(request.id())
                .name(request.name())
                .description(request.description())
                .build();
    }

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
                parameter.getParamType(),
                parameter.getLength(),
                parameter.getMinValue(),
                parameter.getMaxValue(),
                parameter.getOptions().stream().map(this::toOptionResponse).toList()
        );
    }
    public ParameterResponse toParameterResponse(Parameter parameter,String lang) {
        return new ParameterResponse(
                parameter.getId(),
                lang.equals("en") ?  parameter.getName() : parameter.getNameAr(),
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
                parameter.getOptions().stream().map(option -> toOptionResponse(option, lang)).toList()
        );
    }

    public InParameterResponse toInParameterResponse(Parameter parameter) {
        return new InParameterResponse(
                parameter.getId(),
                parameter.getName(),
                parameter.getDescription(),
                parameter.getInternalKey(),
                parameter.getOrderNo(),
                parameter.getRegex(),
                parameter.getInputType(),
                parameter.getJsonType(),
                parameter.getParamType(),
                parameter.getLength(),
                parameter.getMinValue(),
                parameter.getMaxValue(),
                parameter.getOptions().stream().map(this::toOptionResponse).toList(),
                parameter.getExternalKey(),
                parameter.getParameterType(),
                parameter.getIsResponseParam(),
                parameter.getNameAr()
        );
    }

    public OptionResponse toOptionResponse(Option option) {
        return new OptionResponse(
                option.getId(),
                option.getName(),
                option.getValue()
        );
    }

    public OptionResponse toOptionResponse(Option option, String lang) {
        return new OptionResponse(
                option.getId(),
                lang.equals("en") ? option.getName() : option.getNameAr(),//TODO: add check for lang :  lang.equals("en") ?  parameter.getName() : parameter.getNameAr(),
                option.getValue()
        );
    }
}
