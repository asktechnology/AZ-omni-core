package com.az.payment.utils;


import com.az.payment.domain.Parameter;
import com.az.payment.domain.Response;
import com.az.payment.domain.Service;
import com.az.payment.domain.TransactionLog;
import com.az.payment.exception.BusinessException;
import com.az.payment.exception.ValidationException;
import com.az.payment.mapper.RequestMapper;
import com.az.payment.repository.ResponseRepository;
import com.az.payment.request.payment.PostCheckRequest;
import com.az.payment.request.payment.ProcessRequest;
import com.az.payment.response.PaymentResponse;
import com.az.payment.response.ServiceResponse;
import com.az.payment.service.ClientApi;
import com.az.payment.service.ServiceService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

/*
Author a.salah

 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommonUtils {

    @Autowired
    EntityManager entityManager;

    private final ServiceUtils serviceUtils;

    private final ServiceService serviceHelper;
    private final RequestMapper mapper;
    private final ClientApi clientApi;

    private final ResponseRepository responseRepository;

    //Payment Utils
    public int getRRNSequence() {
        // For Oracle, use DUAL to fetch NEXTVAL from sequence
        Object result = entityManager.createNativeQuery("SELECT RRN_SEQ.NEXTVAL FROM DUAL")
                .getSingleResult();
        return ((Number) result).intValue();
    }

    public String generateRRN(long billerId,long serviceId){
        /*
        omniRRN auto generated with format :
        YYMMDDBIBIBISQSQSQSQSQSQSQ
        YY= 2 Digits for Year
        MM= 2 Digits for Month
        DD= 2 Digits for Day
        BIBIBI= 3 Digits for Biller ID (add zeros if less than 3 digits)
        SQSQSQSQSQSQSQ= 7 Digits sequence
        */
        String omniRRN = String.format("%s%02d%03d%07d",
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")),
                billerId,
                serviceId,
                getRRNSequence() //old-> LocalDateTime.now().format(DateTimeFormatter.ofPattern("ssSSS")) //insted of this i want to use the part of second of the current time
        );
        System.out.println("omniRRN : "+omniRRN);
        return omniRRN;
    }

    public JSONObject callDataSource(Parameter parameter, JSONObject requestData){
        JSONObject response = new JSONObject();

        //calling data source service
        ServiceResponse ServiceInfo = serviceHelper.findById(parameter.getDataSourceId());

        ProcessRequest serviceRequest = new ProcessRequest();
        serviceRequest.setId(requestData.getLong("id"));
        serviceRequest.setServiceId(ServiceInfo.id());

        List<Parameter> NonAdditionalInParameters = serviceUtils.getNonAdditionalInParameters(ServiceInfo.id()).toList();
        for (Parameter param:NonAdditionalInParameters) {
            long id = param.getId();
            String key = param.getInternalKey();

            Object objValue = findValueByKey(requestData, key);
            String value = (objValue == null) ? "" : objValue.toString();

            serviceRequest.addParameter(id, key, value);
        }

        JSONObject mappedServiceRequest = mapper.toServiceRequest(serviceRequest, ServiceInfo, requestData.getString("rrn"), "en");

        JSONObject dataServiceResponse = clientApi.callService(mappedServiceRequest, ServiceInfo, -2);

        System.out.println("###########dataServiceResponse");
        System.out.println(dataServiceResponse.toString());
        System.out.println("------------------------------");

        //response.put(parameter.getInternalKey(), "98799955500001");

        return dataServiceResponse;
    }

    public void mapDataSourceParameters(ProcessRequest request, JSONObject mappedRequest, String omniRRN){
        JSONObject requestData = new JSONObject(request.toJson()).put("rrn", omniRRN);

        List<Parameter> parameters = serviceUtils.getDataSourceParameters(request.getServiceId()).toList();

        for (Parameter parameter:parameters) {
            String key = parameter.getExternalKey();
            String value = "";

            JSONObject data = callDataSource(parameter, requestData);
            String retrieveKey = parameter.getInternalKey();
            if(data.has(retrieveKey)){
                Object objValue = data.opt(retrieveKey);
                value = objValue == null ? "": objValue.toString();}
            else{
                log.error("Can't get data for parameter "+key+" : parameter not retrieved by the datasource("+parameter.getDataSourceId()+")");
                throw new BusinessException("Fail");
            }

            mappedRequest.put(key, value);
        }
    }

    // Find value by key in either root or parameters
    public static Object findValueByKey(JSONObject jsonObject, String searchKey) {
        if(jsonObject == null)
            return null;

        // Check root level fields
        if (jsonObject.has(searchKey)) {
            return jsonObject.opt(searchKey);
        }

        // Check parameters array
        if (jsonObject.has("parameters")) {
            JSONArray parameters = jsonObject.getJSONArray("parameters");
            if(parameters != null){
                for (int i = 0; i < parameters.length(); i++) {
                    JSONObject param = parameters.getJSONObject(i);
                    if(param != null)
                        if(param.has("key"))
                            if(!param.isNull("key"))
                                if (param.getString("key").equals(searchKey)) {
                                    if(param.has("value"))
                                        return param.opt("value");
                                }
                }
            }
        }

        return null;
    }

    public int mapResponseCode(long billerId, int externalResponseCode) {
        return Integer.parseInt(responseRepository.findByBillerIdAndExternalResponseCode(billerId, externalResponseCode + "")
                .stream()
                .findFirst()
                .orElseThrow(() -> new ValidationException(format("while mapping the code: response Code %d from biller %d does not have equivalent definition ", externalResponseCode, billerId)))
                .getInternalResponseCode());

    }

    public String mapResponseMessage(long billerId, int externalResponseCode, String lang) {
        Response response = responseRepository.findByBillerIdAndExternalResponseCode(billerId, externalResponseCode + "")
                .stream()
                .findFirst()
                .orElseThrow(() -> new ValidationException(format("while mapping the msg: response Code %d from biller %d does not have equivalent definition ", externalResponseCode, billerId)));

        if (!"en".equals(lang))
            return response.getTranslator().get(lang);
        else
            return response.getDescription();
    }

    public PostCheckRequest generatePostCheckRequest(Service service, TransactionLog transactionLog){
        PostCheckRequest postCheckRequest = new PostCheckRequest();

        postCheckRequest.setId(service.getBillers().stream().findFirst().get().getId());
        postCheckRequest.setServiceId(service.getId());
        postCheckRequest.setOriginOmniRrn(transactionLog.getTransactionId());

        //fill request parameters
        for (Parameter parameter: service.getParameters()) {
            String key = parameter.getInternalKey();

            Object objValue = findValueByKey(new JSONObject(transactionLog.getRequest()), key);
            String value = (objValue == null) ? "" : objValue.toString();

            postCheckRequest.addParameter(parameter.getId(), parameter.getExternalKey(), value);
        }

        return postCheckRequest;
    }

    public String checkResponseStatus(PaymentResponse paymentResponse, long billerId){
        if(paymentResponse.getFinalStatus()==0){//parse long just to make it as same as happened in api
            if(paymentResponse.getResponseCode()==0){//transaction passed
                //TODO: may be ResponseCode param is not the param that holding the status, should find scenario for that
                return "TRANSUCCESS";
            } else if(paymentResponse.getResponseCode()==10){//transaction still timeout
                return "TRANTIMEOUT";
            } else{ //transaction failed
                return "TRANFAIL";
            }
        } else if(paymentResponse.getFinalStatus()==-2){//biller call timeout
            return "CALLTIMEOUT";
        } else if(paymentResponse.getFinalStatus()==-1){ //biller call failed
            if(paymentResponse.getResponseCode()==-11 ||
                    paymentResponse.getResponseCode()==-12 ||
                    paymentResponse.getResponseCode()==-13 ||
                    paymentResponse.getResponseCode()==-14 ) //TODO:should be taken from DB
                return "CALLFAILED";
            else {
                List<Response> internalResponseCode = responseRepository.findByBillerIdAndExternalResponseCode(billerId, String.valueOf(paymentResponse.getResponseCode()));
                if(internalResponseCode.isEmpty())
                    return "CALLFAILED";
                else
                    return "TRANFAIL";
            }
        } else
            return "CALLFAILED";
    }
}
