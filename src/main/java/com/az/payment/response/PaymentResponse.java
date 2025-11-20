package com.az.payment.response;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PaymentResponse {

    @JsonAlias("response_params")
    @JsonProperty("response_params")
    private List<PaymentResponseField> responseParams;
    @JsonAlias("final_status")
    @JsonProperty("final_status")
    private long finalStatus;
    @JsonAlias("is_payment")
    @JsonProperty("isPayment")
    private boolean isPayment = false;
    @JsonAlias("request_params")
    @JsonProperty("request_params")
    private List<ParameterResponse> requestParams;
    private int responseCode;
    private String responseMessage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bankReference;

}
