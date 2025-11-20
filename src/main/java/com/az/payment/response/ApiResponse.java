package com.az.payment.response;

import com.az.payment.constants.Code;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse implements Serializable {

    private boolean success;
    @JsonProperty("responseCode")
    private Integer code = Code.SUCCESS.getResponseCode();
    @JsonProperty("responseMessage")
    private String message = "";
    private Object data;

    public ApiResponse(Object data) {

        this.data = data;
    }

    public ApiResponse(Code code, String message) {
        this.success = false;
        this.code = code.getResponseCode();
        this.message = message;
    }

    public ApiResponse(Code code, Object data) {
        this.data = data;
        this.success = code.getResponseCode() == Code.SUCCESS.getResponseCode();
        this.code = code.getResponseCode();
        this.message = code.getMessage();

    }
}
