package com.az.payment.service;

import com.az.payment.exception.BusinessException;
import com.az.payment.repository.BillerRepository;
import com.az.payment.repository.ServiceRepository;
import com.az.payment.response.InParameterResponse;
import com.az.payment.response.ServiceResponse;
import com.az.payment.utils.BillerUtils;
import com.az.payment.utils.ServiceUtils;
import io.netty.channel.ConnectTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientApi {

    @Value("${timeout.biller.connection}")
    private long connectionTimeout;

    @Value("${timeout.biller.read}")
    private long readTimeout;

    private final BillerRepository repository;
    private final ServiceRepository serviceRepository;
    private final ServiceService serviceService;
    private final ServiceUtils serviceUtils;
    private final BillerUtils billerUtils;

    public JSONObject callService(JSONObject mappedRequest, ServiceResponse processService, long billerId) {
        // should check if request is GET or POST first
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(mappedRequest.toString(), headers);

            var service = serviceRepository.findById(processService.id());

            if (service.isEmpty())
                throw new BusinessException(String.format("Service WIth ID Does not exists %d", processService.id()));

            var biller = repository.findById(billerId);
            if (biller.isEmpty())
                throw new BusinessException(String.format("Biller WIth ID Does not exists %d", billerId));
            String requestURI = biller.get().getBaseUrl() + service.get().getServicePath();

            RestTemplate template = restTemplate(connectionTimeout,readTimeout);

            var response = template.exchange(requestURI , HttpMethod.POST, request, String.class);

            log.info("ClientAPi::CallService response: {}", response.getBody());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                return new JSONObject(response.getBody());
            }
            else {
                return getExceptionResponse(
                        response.getStatusCode().value(),
                        -1,
                        Objects.requireNonNull(response.getBody()).isEmpty() ? "" : response.getBody(),
                        "error occurred in Server Call");
            }

        } catch (ResourceAccessException ex) {
            // This exception is thrown for connection and read timeouts
            if (ex.getCause() != null && ex.getCause() instanceof SocketTimeoutException) {
                // Read timeout occurred
                // we should return valid response body
                // we will consider the biller is successful (old decision)
//                return getSuccessResponse(billerId, processService.id());

                //return response with special status(-2) (new decision)
                return getExceptionResponse(-2, -10, "", "Biller not responding: "+ex.getMessage());
            } else if (ex.getCause() != null && ex.getCause() instanceof ConnectTimeoutException) {
                // Connection timeout occurred (status:failed)
                return getExceptionResponse(-1, -11, "", "Connection timeout occurred while calling Biller: "+ex.getMessage());
            }
            // Other ResourceAccessException cases (status:failed)
            return getExceptionResponse(-1, -12, "", "Failed to access the Biller: "+ex.getMessage());
        }
        catch (RestClientException ex) {
            // Handle other RestClientExceptions
            return getExceptionResponse(-1, -13, "", "Biller call failed: " + ex.getMessage());
        }
        catch (Exception e) {
            return getExceptionResponse(-1, -14, "", e.getMessage());
        }

    }

    private JSONObject getExceptionResponse(int status, int code, String body, String message){
        JSONObject exceptionResponse = new JSONObject();
        exceptionResponse.put("status", status);
        exceptionResponse.put("code", code);
        exceptionResponse.put("message", message);
        log.error(message);
        return exceptionResponse;
    }

    private JSONObject getSuccessResponse(long billerId, long serviceID){
        JSONObject successResponse = new JSONObject();
        List<InParameterResponse> parameters = serviceService.findResponseParameterByServiceId(serviceID);
        for (InParameterResponse parameter : parameters) {
            String key = parameter.externalKey();
            String valueSTRING = "";
            int valueINT = 0;

            //fill with success details
            if(key.equals(serviceUtils.getSpecialParameter(serviceID, "resCode").getExternalKey())){
                valueSTRING= billerUtils.getBillerSuccessCode(billerId);
                successResponse.put(key, valueSTRING);
            }
            else if(key.equals(serviceUtils.getSpecialParameter(serviceID, "resMsg").getExternalKey())){
                valueSTRING= billerUtils.getBillerSuccessMsg(billerId);
                successResponse.put(key, valueSTRING);
            }
            else
                switch (parameter.parameterDataType()) {
                    case STRING:
                        successResponse.put(key, valueSTRING);
                        break;
                    case INT:
                        successResponse.put(key, valueINT);
                        break;
                    default:
                        successResponse.put(key, valueSTRING);
                        break;
                }
        }
        return successResponse;
    }

    public RestTemplate restTemplate(long connectTimeout, long readTimeout) {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(connectTimeout))// Timeout to establish connection
                .setReadTimeout(Duration.ofSeconds(readTimeout))// Timeout waiting for data
                .build();
    }
}
