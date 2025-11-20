package com.az.payment.request.payment;


import com.az.payment.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ProcessRequest {

    private long id;
    private long serviceId;
    private List<RequestParameter> parameters;
    private String accountFrom;

    //a.salah 1/5/2025
    public ProcessRequest addParameter(long id, String key, String value){
        if(this.parameters == null)
            this.parameters = new ArrayList<>();

        RequestParameter parameter = new RequestParameter(id, key, value);
        this.parameters.add(parameter);

        return this;
    }

    //a.salah jul2025 Convert to JSON string
    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        }catch (JsonProcessingException e){
            throw new RuntimeException("Error While processing request");
        }
    }

    //a.salah jul2025 Convert to JsonNode
    public JsonNode toJsonNode(){
            return new ObjectMapper().valueToTree(this);
    }
}
