package com.az.payment.utils;


import com.az.payment.domain.JcardOmniTransaction;
import com.az.payment.domain.Parameter;
import com.az.payment.domain.Response;
import com.az.payment.repository.JcardOmniTransactionRepository;
import com.az.payment.repository.ResponseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillerUtils {

    private final ResponseRepository responseRepository;
    private final JcardOmniTransactionRepository jcardOmniTransactionRepository;

    private final ServiceUtils serviceUtils;

    //Response Utils
    public String getBillerSuccessCode(long billerId){
        String notFoundCode = "-1101";
        String billerResCode=notFoundCode;

        billerResCode = responseRepository.findByBillerIdAndIsSuccess(billerId, 1)
                .stream()
                .findFirst()
                .orElse(new Response().builder().externalResponseCode(notFoundCode).build())
                .getExternalResponseCode();

        return billerResCode;
    }
    public String getBillerTimeoutCode(long billerId){
        String billerResCode="no code";

        billerResCode = responseRepository.findByBillerIdAndIsTimeout(billerId, 1)
                .stream()
                .findFirst()
                .orElse(new Response().builder().externalResponseCode("no code").build())
                .getExternalResponseCode();

        return billerResCode;
    }

    public String getBillerSuccessMsg(long billerId){
        String billerResMsg="no msg";

        billerResMsg = responseRepository.findByBillerIdAndIsSuccess(billerId, 1)
                .stream()
                .findFirst()
                .orElse(new Response().builder().description("no msg").build())
                .getDescription();
        return billerResMsg;
    }

    //old but usefull
    public boolean updateBillerTrnSts(JSONObject response, String rrn, long billerId, long serviceId){

        JcardOmniTransaction jca =
                jcardOmniTransactionRepository.findByRrn(rrn)
                        .orElseThrow(() -> new EntityNotFoundException(format("Payment failed: no payment record initiated ", rrn)));

        ////set biller details
        //get the keys
        List<Parameter> parameters = serviceUtils.getSpecialParameters(serviceId);
        String extRrnKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "extRrn").getExternalKey(), "");
        String resCodeKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "resCode").getExternalKey(), "");
        String resMsgKey = Objects.requireNonNullElse(serviceUtils.getSpecialParameter(parameters, "resMsg").getExternalKey(), "");

        //get the values
        //if biller doesn't have the _key_ put no _key_
        String extRrn = response.optString(extRrnKey,"no extRrn");
        String resCode = response.optString(resCodeKey,"no resCode");
        String resMsg = response.optString(resMsgKey,"no resMsg");

        //set universal success code and msg
        //TODO: if there is no success code set it = "0"  to avoid NullPointerException
        String billerSuccCode = getBillerSuccessCode(billerId);

        if(billerSuccCode=="no code"){
            log.info("No success code found in DB for Biller("+billerId+")");
        }

        if(billerSuccCode.equals(resCode)){
            resCode = "00001";
            resMsg = "Success";
        }

        jca.setExternalRrn(extRrn);
        jca.setBillerResponse(resCode);
        jca.setBillerMessage(resMsg);

        jcardOmniTransactionRepository.save(jca);
        return true;
    }

    public boolean updateBillerTrnSts(String rrn, String extRrn, String resCode, String resMsg, String billerSuccessCode){

        //get trn row
        JcardOmniTransaction jca =
                jcardOmniTransactionRepository.findByRrn(rrn)
                        .orElseThrow(() -> new EntityNotFoundException(format("Payment failed: no payment record initiated ", rrn)));

        ////set biller details
        jca.setExternalRrn(extRrn);
        jca.setBillerResponse(resCode.equals(billerSuccessCode)?"00001" : resCode);
        jca.setBillerMessage(resMsg);

        jcardOmniTransactionRepository.save(jca);
        return true;
    }

}
