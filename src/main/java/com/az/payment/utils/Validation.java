package com.az.payment.utils;

import com.az.payment.domain.Parameter;
import com.az.payment.domain.Response;
import com.az.payment.exception.BusinessException;
import com.az.payment.exception.ValidationException;
import com.az.payment.repository.ResponseRepository;
import com.az.payment.request.payment.ProcessRequest;
import com.az.payment.request.payment.RequestParameter;
import com.az.payment.response.*;
import com.az.payment.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
@Slf4j
public class Validation {

    private final ServiceService service;
    private final ParameterUtils helper;
    private final ServiceUtils serviceUtils;
    private final CommonUtils commonUtils;

    public void validateRequest(ProcessRequest request,String locale) {
        log.info("Validating request");
        int count = 0;
        boolean found = false;
        StringBuilder notFoundParams = new StringBuilder();
        // 1- first validate all required parameters are available
        var requestParameters = request.getParameters();
        var serviceParameters = service.findParameterByServiceId(request.getServiceId(),locale);
        log.info("Validating request parameters in request against actual parameters counts  {}", serviceParameters.size());
        for (var serviceParameter : serviceParameters) {
            found = false;
            for (var requestParameter : requestParameters) {
                if (serviceParameter.id() == requestParameter.id()) {
                    count++;
                    found = true;
                }
            }
            if (!found) {
                notFoundParams.append(format("request parameter %s with Id %d not found in request ", serviceParameter.name(), serviceParameter.id())).append(",,,");
            }
        }
        log.info("Found {} parameters in request ,,,against actual parameters counts  {}", count, serviceParameters.size());

        if (count != serviceParameters.size()) {
            log.info("Validation failed :: {}", notFoundParams.toString());
            throw new ValidationException(format("Validation failed for request %s ,,, request parameters not found ::: %s", request, notFoundParams.toString()));
        }
        // check parameters individually

        var validatingParams = requestParameters.stream()
                .map(this::validateParameter)
                .toList();
        // should not reach here if params are not valid
        log.info("Validation::validateRequest After validating params against its definition ");

    }

    public Boolean validateParameter(RequestParameter requestParameter) {
        log.info("Validating parameter {} ::: with value {}", requestParameter.key(), requestParameter.value());
        var parameter = helper.findById(requestParameter.id());
        // check parameter length
        if (parameter.length() != -1 && (requestParameter.value().length() > parameter.length() || requestParameter.value().length() < parameter.length()))
            throw new ValidationException(format("Invalid value for parameter %s ,,, with value %s  actual Size %d -> size found %d", requestParameter.key(), requestParameter.value(), parameter.length(), requestParameter.value().length()));
        if (parameter.minValue() != -1 && Double.parseDouble(requestParameter.value()) < parameter.minValue())
            throw new BusinessException(format("Invalid value for parameter %s ,,, with value %s ,,,, value is Less Than %f ", requestParameter.key(), requestParameter.value(), parameter.minValue()));
        if (parameter.maxValue() != -1 && Double.parseDouble(requestParameter.value()) > parameter.maxValue())
            throw new BusinessException(format("Invalid value for parameter %s ,,, with value %s ,,,, value is Greater Than %f ", requestParameter.key(), requestParameter.value(), parameter.minValue()));
        if (parameter.regex() != null && !parameter.regex().isEmpty() && !requestParameter.value().matches(parameter.regex()))
            throw new BusinessException(format("Invalid value for parameter %s ,,, with value %s ,,,, value does not matches defied regex [%s]", requestParameter.key(), requestParameter.value(), parameter.regex()));

        return true;
    }

    public PaymentResponse validateResponse(JSONObject apiResponse, ServiceResponse processService, long billerId,String locale) {
        log.info("Validation::validateResponse Validating response  ,,, apiResponse = {},,,, for service ID {} , with name {}",
                apiResponse, processService.id(), processService.name());
        StringBuilder notFoundInResponse = new StringBuilder();
        var parameters = service.findResponseParameterByServiceId(processService.id());
        PaymentResponse paymentResponse = new PaymentResponse();
        List<PaymentResponseField> response = new ArrayList<>();

        Parameter resCodeParameter = serviceUtils.getSpecialParameter(processService.id(),"resCode");
        int resCode = apiResponse.has(resCodeParameter.getExternalKey())?apiResponse.getInt(resCodeParameter.getExternalKey()):0;

        /// should map response against parameters defined
        for (var responseParameter : parameters) {
            log.info("parameter in List is {},,,, first check {} , second check {} ", responseParameter.externalKey(), apiResponse.has(responseParameter.externalKey()) && !apiResponse.isNull(responseParameter.externalKey()) && !responseParameter.isResponseParam(), apiResponse.has(responseParameter.externalKey()) && !apiResponse.isNull(responseParameter.externalKey()) && !responseParameter.isResponseParam());
            if (apiResponse.has(responseParameter.externalKey()) && !apiResponse.isNull(responseParameter.externalKey()) && !responseParameter.isResponseParam()) {
                constructResponse(apiResponse, response, responseParameter, billerId, processService.id(), resCode, locale);
            } else if (apiResponse.has(responseParameter.externalKey()) && !apiResponse.isNull(responseParameter.externalKey()) && responseParameter.isResponseParam()) {
                try{
                    Boolean isResponseCodeParameter = responseParameter.externalKey().equals(resCodeParameter.getExternalKey());
                    if(isResponseCodeParameter) {
                        switch (responseParameter.parameterDataType()) {
                            case STRING:
                                // should add this as method in responseService as to create a redis-cache store rather than use database always with everyRequest
                                paymentResponse.setResponseCode(commonUtils.mapResponseCode(billerId, Integer.parseInt(apiResponse.getString(responseParameter.externalKey()))));
                                paymentResponse.setResponseMessage(commonUtils.mapResponseMessage(billerId, Integer.parseInt(apiResponse.getString(responseParameter.externalKey())), locale));
                                break;
                            case INT:
                            default:
                                paymentResponse.setResponseCode(commonUtils.mapResponseCode(billerId, apiResponse.getInt(responseParameter.externalKey())));
                                paymentResponse.setResponseMessage(commonUtils.mapResponseMessage(billerId, apiResponse.getInt(responseParameter.externalKey()), locale));
                                break;
                        }
                    }else
                        log.info("parameter["+responseParameter.externalKey()+"] not a response code from biller");
                }catch (ValidationException e){
                    log.info(e.getMessage());
                }
            } else {
                notFoundInResponse.append(responseParameter.externalKey()).append(",,,");
            }
        }
        if (!notFoundInResponse.isEmpty()) {
//a.salah            throw new BusinessException(format("Parameters not found in response %s ", notFoundInResponse.toString()));
            log.error(format("Parameters not found in response %s ", notFoundInResponse.toString()));
        }
        paymentResponse.setResponseParams(response);

        log.info("response returned after mapping data is {} ", response);
        return paymentResponse;
    }

    // method  receives parameter type and response then extract the value
    private void constructResponse(JSONObject apiResponse, List<PaymentResponseField> response, InParameterResponse responseParameter, long billerId, long serviceId, int resCode, String locale) {

        PaymentResponseField field = switch (responseParameter.parameterDataType()) {
            case INT ->
                    new PaymentResponseField(responseParameter.internalKey(), apiResponse.getInt(responseParameter.externalKey()), locale.equals("en") ? responseParameter.name() : responseParameter.nameAr(), responseParameter.id());
            case BOOLEAN ->
                    new PaymentResponseField(responseParameter.internalKey(), apiResponse.getBoolean(responseParameter.externalKey()), locale.equals("en") ? responseParameter.name() : responseParameter.nameAr(), responseParameter.id());
            case DOUBLE ->
                    new PaymentResponseField(responseParameter.internalKey(), apiResponse.getDouble(responseParameter.externalKey()), locale.equals("en") ? responseParameter.name() : responseParameter.nameAr(), responseParameter.id());
            default ->
                    new PaymentResponseField(responseParameter.internalKey(), apiResponse.getString(responseParameter.externalKey()), locale.equals("en") ? responseParameter.name() : responseParameter.nameAr(), responseParameter.id());
        };

        Parameter responseMsgParameter = serviceUtils.getSpecialParameter(serviceId,"resMsg");
        Boolean isResponseMsgParameter = responseParameter.externalKey().equals(responseMsgParameter.getExternalKey());
        if(isResponseMsgParameter){
            field = new PaymentResponseField(field.key(), commonUtils.mapResponseMessage(billerId, resCode, locale), field.displayName(), field.id());
        }

        response.add(field);
    }

    public void validatePayment() {
    }

    public void validateTransaction() {
    }

}