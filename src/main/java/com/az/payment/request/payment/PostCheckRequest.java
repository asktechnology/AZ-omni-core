package com.az.payment.request.payment;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PostCheckRequest {

    private long id;
    private long serviceId;
    private List<RequestParameter> parameters;
    private String originOmniRrn;

    public PostCheckRequest addParameter(long id, String key, String value){
        if(this.parameters == null)
            this.parameters = new ArrayList<>();

        RequestParameter parameter = new RequestParameter(id, key, value);
        this.parameters.add(parameter);

        return this;
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        }catch (JsonProcessingException e){
            throw new RuntimeException("Error While processing request");
        }
    }

    public JsonNode toJsonNode(){
            return new ObjectMapper().valueToTree(this);
    }
}
