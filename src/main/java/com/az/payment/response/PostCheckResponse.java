package com.az.payment.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PostCheckResponse {

    @JsonAlias("response_params")
    @JsonProperty("response_params")
    private List<PaymentResponseField> responseParams;
    @JsonAlias("final_status")
    @JsonProperty("final_status")
    private long finalStatus;

    private int responseCode;
    private String responseMessage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bankReference;

}
