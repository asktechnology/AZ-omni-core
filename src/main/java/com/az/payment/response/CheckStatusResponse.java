package com.az.payment.response;

import com.az.payment.request.payment.RequestParameter;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/*
author a.salah

*/
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CheckStatusResponse {

    private long responseCode;
    private String responseMessage;
    private long trnStatus;
    private String trnStatusDisc;
    private String transactionId;
    private String checkReqId;
    private List<PaymentResponseField> trnDetails;

    public CheckStatusResponse addTrnDetail(String key, Object value, String displayName, long id){
        if(this.trnDetails == null)
            this.trnDetails = new ArrayList<>();

        PaymentResponseField detail = new PaymentResponseField(key, value, displayName, id);
        this.trnDetails.add(detail);

        return this;
    }
}
