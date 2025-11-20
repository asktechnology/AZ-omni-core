package com.az.payment.mapper;

import com.az.payment.response.PaymentResponse;
import com.az.payment.response.ServiceResponse;
import com.az.payment.utils.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;


/*
author a.salah
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseMapper {
    private final Validation validation;

    public PaymentResponse toServiceResponse(JSONObject apiResponse, ServiceResponse processService, long billerId, String locale) {

        //TODO: validation should be seperated from response mapping to avoid dependancy injection loop
        // should validate successful response && prepare response from Mapping
        PaymentResponse response = validation.validateResponse(apiResponse, processService, billerId,locale);
        // should map response and generate new request Parameters if this service in  Query Stage
        log.info("response returned from mapper and validated as defined  {}", response);
        return response;
    }
}
