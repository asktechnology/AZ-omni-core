package com.az.payment.mapper;

import com.az.payment.domain.Parameter;
import com.az.payment.domain.ParameterDataType;
import com.az.payment.domain.ServiceType;
import com.az.payment.exception.BusinessException;
import com.az.payment.repository.ParameterRepository;
import com.az.payment.repository.ServiceRepository;
import com.az.payment.request.payment.ProcessRequest;
import com.az.payment.request.payment.RequestParameter;
import com.az.payment.response.PaymentResponse;
import com.az.payment.response.ServiceResponse;
import com.az.payment.service.ServiceService;
import com.az.payment.utils.CommonUtils;
import com.az.payment.utils.ServiceUtils;
import com.az.payment.utils.Validation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestMapper {

    private final ParameterRepository repository;
    private final ServiceRepository serviceRepository;
    private final ParameterRepository parameterRepository;
    private final ParametersMapper mapper;
    private final ServiceService serviceService;
    private final ServiceUtils serviceUtils;

    public JSONObject toServiceRequest(ProcessRequest request, ServiceResponse processService, String omniRRN, String locale) {
        log.info("start Mapping Request .... {}", request);
        // TODO :: change mapping to HASHMAP<STRING,OBJECT> as referred by Imam
        // should take the Json return from the Mapped function and then add additional Data

        //get the service
        var service = serviceRepository.findById(processService.id())
                .orElseThrow(() -> new BusinessException("Service not found"));
        List<Parameter> serviceParameters = service.getParameters();

        //filter the request:
        //first check if there is missing 'In' parameters
        List<Long> reqParIds = request.getParameters().stream().map(p -> {return p.id();}).toList();
        ////get just input parameters those are not additional parameters
        List<Parameter> serviceNonAdditionalInParameters = serviceParameters.stream().
                filter(p -> {return serviceUtils.isNonAdditionalInParameter(p);}).toList();
        serviceNonAdditionalInParameters.stream()
                .forEach(parameter -> {
                    //if the parameter is missing
                    if(!reqParIds.contains(parameter.getId())){
                        log.error("Parameter["+parameter.getInternalKey()+".id("+parameter.getId()+")] not found in request");
                        throw new BusinessException("Invalid Request");
                    }
                });

        //second remove unwanted parameters
        List<Long> serviceNonAdditionalInParametersIds = serviceNonAdditionalInParameters.stream().map(p -> {return p.getId();}).toList();
        List<RequestParameter> requestFilteredParameters = request.getParameters().stream().filter(
                parameter -> {
                    if (!serviceNonAdditionalInParametersIds.contains(parameter.id())){
                        log.warn("found in request : not valid Parameter["+parameter.key()+".id("+parameter.id()+")] ");
                        return false;
                    }
                    return true;

                }).toList();

        //Set ReqRrn as 'service definition' or ['default one' even it's not required as 'service definition']
        Parameter reqRrnParameter = getReqRrnParameter(request.getServiceId());
        String reqRrnStr = reqRrnParameter.getReqrrnRegex().replaceAll("%RRN%", omniRRN);
        log.info("reqRrn : "+reqRrnParameter.getId()+" : "+reqRrnParameter.getExternalKey()+" : "+reqRrnStr);

        log.info("start Mapping Additional Request Data Such as fixed Value and transaction ID if available ");
        var additionalParams = serviceParameters.stream().
                filter(
                        parameter -> {
                            // in filter should be filtered By either its fixed or its generated
                            boolean isGeneratedId = parameter.isGeneratedTransactionId();
                            boolean isFixed = parameter.getIsFixed() == 1;
                            if((parameter.getParamType() == 1 || parameter.getParamType() == 2) &&
                                    (isFixed || isGeneratedId)){
                                log.info(parameter.getExternalKey() + " : " + (isGeneratedId?"GeneratedId":isFixed?"fixed":"other"));
                                return true;
                            }
                            return false;
                        })
                .map(mapper::toRequestParameter)
                .toList();

        //constructing the final set of request parameters
        HashMap<String, Object> params = toJsonField(requestFilteredParameters, serviceNonAdditionalInParameters);
        params.putAll(toJsonFieldFixedValues(additionalParams));
        params.put(reqRrnParameter.getExternalKey(), reqRrnStr);
        log.info("end Mapping Request .... {}", new JSONObject(params));
        return new JSONObject(params);
    }


    // map request to its values and data Types
    private HashMap<String, Object> toJsonField(List<RequestParameter> requestParameters) {
        var params = new HashMap<String, Object>();

        if(requestParameters == null)
            return params;//a.salah avoid null exception

        for (RequestParameter requestParameter : requestParameters) {
            //TODO:pass 'serviceNonAdditionalInParameters' as parameter and change bellow line by getting the parameter from the list instead of DB
            var parameter = repository.findById(requestParameter.id());
            //TODO:below check code becomes unnecessary after the precheck added in toServiceRequest
            if (parameter.isEmpty())
                throw new RuntimeException("Parameter not found");
            if (!(parameter.get().getParamType()==1 || parameter.get().getParamType()==3)){
                log.info("Parameter |"+requestParameter.key()+"|id("+requestParameter.id()+")| is not IN parameter");
                continue;
            }

            //a.salah perform simplicity by using named variables
            ParameterDataType parameterType = parameter.get().getParameterType();
            String externalKey = parameter.get().getExternalKey();
            String value = requestParameter.value();

            if (parameterType == null)
                params.put(externalKey, value);
            else if(value == null || value == "")//a.salah avoid null exception
                params.put(externalKey, value);
            else if (parameterType == ParameterDataType.BOOLEAN)
                params.put(externalKey, Boolean.parseBoolean(value));
            else if (parameterType == ParameterDataType.INT) {
                log.info("inside toJsonFIeld {}", value.matches("[0-9.]+"));
                params.put(externalKey, Integer.parseInt(value));
            } else if (parameterType == ParameterDataType.DATE) {
                try {
                    params.put(externalKey, new SimpleDateFormat("dd/MM/yyyy").parse(value));
                } catch (ParseException e) {
                    throw new BusinessException(String.format("Invalid date format: %s for Key %s", value, requestParameter.key()));
                }
            } else if (parameterType == ParameterDataType.STRING)
                params.put(externalKey, value);
            else if (parameterType.equals(ParameterDataType.DOUBLE))
                params.put(externalKey, Double.parseDouble(value));
        }
        return params;
    }

    // map request to its values and data Types
    private HashMap<String, Object> toJsonField(List<RequestParameter> requestParameters, List<Parameter> serviceNonAdditionalInParameters) {
        var params = new HashMap<String, Object>();

        if(requestParameters == null)
            return params;//a.salah avoid null exception

        for (RequestParameter requestParameter : requestParameters) {
            //TODO:pass 'serviceNonAdditionalInParameters' as parameter and change bellow line by getting the parameter from the list instead of DB
            var parameter = serviceNonAdditionalInParameters.stream().filter(p -> p.getId()==requestParameter.id()).findFirst();

            if (parameter.isEmpty())
                throw new RuntimeException("Parameter["+requestParameter.key()+".id("+requestParameter.id()+")] not valid 'In' Parameter ");

            //a.salah perform simplicity by using named variables
            ParameterDataType parameterType = parameter.get().getParameterType();
            String externalKey = parameter.get().getExternalKey();
            String value = requestParameter.value();

            if (parameterType == null)
                params.put(externalKey, value);
            else if(value == null || value == "")//a.salah avoid null exception
                params.put(externalKey, value);
            else if (parameterType == ParameterDataType.BOOLEAN)
                params.put(externalKey, Boolean.parseBoolean(value));
            else if (parameterType == ParameterDataType.INT) {
                log.info("inside toJsonFIeld {}", value.matches("[0-9.]+"));
                params.put(externalKey, Integer.parseInt(value));
            } else if (parameterType == ParameterDataType.DATE) {
                try {
                    params.put(externalKey, new SimpleDateFormat("dd/MM/yyyy").parse(value));
                } catch (ParseException e) {
                    throw new BusinessException(String.format("Invalid date format: %s for Key %s", value, requestParameter.key()));
                }
            } else if (parameterType == ParameterDataType.STRING)
                params.put(externalKey, value);
            else if (parameterType.equals(ParameterDataType.DOUBLE))
                params.put(externalKey, Double.parseDouble(value));
        }
        return params;
    }

    private HashMap<String, Object> toJsonFieldFixedValues(List<RequestParameter> requestParameters) {
        var json = new HashMap<String, Object>();

        if(requestParameters == null)
            return json;//a.salah avoid null exception

        for (RequestParameter requestParameter : requestParameters) {
            if (Objects.requireNonNullElse(requestParameter.value(),"").isEmpty())//a.salah avoid null exception
                json.put(requestParameter.key(), String.valueOf(System.nanoTime()));
            else
                json.put(requestParameter.key(), requestParameter.value());
        }
        return json;
    }

    public PaymentResponse toClientResponse(PaymentResponse response, ProcessRequest request,String locale) {

        log.info("start toClientResponse .... {},,,,, with request ............{} ", response, request);
        // get service by Service ID
        var service = serviceRepository.findById(request.getServiceId());
        List<Long> ids = request.getParameters().stream().map(RequestParameter::id).toList();
        if (service.isEmpty())
            return response;

        if(service.get().getServiceType() == ServiceType.BOTH)
            if (service.get().getAfterServiceId() > 0) {
                var nextServiceId = service.get().getAfterServiceId();
                response.setFinalStatus(nextServiceId);
                var nextService = serviceRepository.findById(nextServiceId).orElseThrow(() -> new BusinessException("Service not found"));
                response.setRequestParams(serviceService.findParameterByServiceId(nextServiceId,locale)
                        .stream().filter(param -> !ids.contains(param.id())).toList());
                response.setPayment(nextService.isPayment());
            } else if (service.get().getAfterServiceId() == 0 && service.get().isPayment()) {
                response.setFinalStatus(0);
                response.setRequestParams(null);
                response.setPayment(false);
            } else {
                response.setFinalStatus(-1);
                response.setRequestParams(null);
                response.setPayment(false);
            }

        // check if service has after service
        // if so then check is it isPayment or not

        return response;
    }

    private Parameter getReqRrnParameter(long serviceId){
        //for all services we will send ID for tracking even if it's not required
        //TODO:if the service required an id in request we will go with the service definition
        com.az.payment.domain.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(format("Service Not found By Id %d", serviceId)));
        List<Parameter> serviceParameters = Objects.requireNonNullElse(service.getParameters(), new ArrayList<Parameter>());
        Parameter defaultReqRrnParameter = parameterRepository.findById(Long.parseLong("-1")).orElse(new Parameter());
        Parameter reqRrnParameter = serviceParameters.stream().filter(parameter -> parameter.getIsReqrrn()==1).findFirst()
                .orElse(defaultReqRrnParameter);

        return reqRrnParameter;
    }

}
