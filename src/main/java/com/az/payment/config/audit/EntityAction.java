package com.az.payment.config.audit;

import com.az.payment.constants.Actions;
import com.az.payment.constants.EntityType;
import com.az.payment.response.PaymentResponse;
import lombok.Getter;


@Getter
public enum EntityAction {
    QUESTION_CREATE(EntityType.QUESTION, Actions.CREATE, PaymentResponse.class);

    private final Integer action;
    private final Integer entity;
    private final Class<?> idClass;
    private final String messageSuffix;

    EntityAction(Integer entity, Integer action, Class<?> idClass) {
        this.action = action;
        this.entity = entity;
        this.idClass = idClass;
        this.messageSuffix = this.name().toLowerCase().replace('_', '.');
    }

}
