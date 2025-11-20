package com.az.payment.request;

import com.az.payment.config.audit.EntityAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Map;


@Getter
@Setter
public class AuditMessage {

    private EntityAction auditValue;
    private Map<String, String> data;
    private Date auditDate;

    public AuditMessage() {
        this.auditDate = new Date();
    }

    public AuditMessage(

            EntityAction auditValue,
            Map<String, String> data) {
        this();
        this.auditValue = auditValue;
        this.data = data;
    }
}
