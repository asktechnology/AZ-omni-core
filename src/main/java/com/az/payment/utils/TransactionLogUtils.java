package com.az.payment.utils;

import com.az.payment.domain.Parameter;
import com.az.payment.domain.TransactionLog;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Component
@Slf4j
@Transactional
public class TransactionLogUtils {

    @Autowired
    ServiceUtils serviceUtils;

    private TransactionLog setRequestDetails(TransactionLog transactionLog){

        String request = transactionLog.getRequest();

        if(request == null)
            return transactionLog.builder().request("null req").build();

        JSONObject jsonObject = new JSONObject(request);

        //get the keys
        List<Parameter> parameters = serviceUtils.getSpecialParameters(transactionLog.getServiceId());
        String billIdKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "billId").getExternalKey(), "");
        String amountKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "amount").getExternalKey(), "");

        //get the values
        transactionLog.setVoucherNo(jsonObject.optString(billIdKey,"no voucherNo"));
        transactionLog.setAmount(jsonObject.optInt(amountKey,-1));
        return transactionLog;
    }

    private TransactionLog setResponseDetails(TransactionLog transactionLog){
        String response = transactionLog.getResponse();

        if(response == null){
            return transactionLog.builder().response("null res").build();
        }

        JSONObject jsonResponse = new JSONObject(response);

        //get the keys
        List<Parameter> parameters = serviceUtils.getSpecialParameters(transactionLog.getServiceId());
        String extRrnKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "extRrn").getExternalKey(), "");
        String resCodeKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "resCode").getExternalKey(), "");
        String resMsgKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "resMsg").getExternalKey(), "");

        //get the values
        transactionLog.setExtRrn(jsonResponse.optString(extRrnKey,"no extRrn"));
        transactionLog.setResponseCode(jsonResponse.optString(resCodeKey,"no resCode"));
        transactionLog.setResponseMessage(jsonResponse.optString(resMsgKey,"no resMsg"));

        return transactionLog;
    }

    public long getParameterId(long serviceId, String key){
        return serviceUtils.getParameterIdByExtKey(serviceId,key);
    }

    public TransactionLog parseRequest(TransactionLog transactionLog, String request){
        transactionLog.setRequest(request);

        transactionLog = setRequestDetails(transactionLog);

        transactionLog = parseBody(transactionLog, 0);

        return transactionLog;
    }

    public TransactionLog parseResponse(TransactionLog transactionLog, String response){
        transactionLog.setResponse(response);

        transactionLog = setResponseDetails(transactionLog);

        transactionLog = parseBody(transactionLog, 1);

        return transactionLog;
    }

    private TransactionLog parseBody(TransactionLog transactionLog, int reqResFlag){
        String body = "";
        if(reqResFlag==0)
            body = transactionLog.getRequest();
        else if(reqResFlag==1)
            body = transactionLog.getResponse();

        JSONObject jsonObject = new JSONObject(body);
        for (String key : jsonObject.keySet()) {
            String value = Objects.toString(key, "");

            long paramId = getParameterId(transactionLog.getServiceId(), key);

            transactionLog.addChainTransactionLog(paramId, reqResFlag, key, value);
        }

        return transactionLog;
    }

}
