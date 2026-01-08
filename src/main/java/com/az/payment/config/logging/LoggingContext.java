package com.az.payment.config.logging;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LoggingContext {
    private static final String UUID_KEY = "uuid";
    private static final String BILLER_NAME_KEY = "billerName";
    private static final String SERVICE_NAME_KEY = "serviceName";

    public void generateAndSetUUID() {
        String uuid = UUID.randomUUID().toString();
        MDC.put(UUID_KEY, uuid);
    }

    public void setUUID(String uuid) {
        MDC.put(UUID_KEY, uuid);
    }

    public String getUUID() {
        return MDC.get(UUID_KEY);
    }

    public void setBillerName(String billerName) {
        MDC.put(BILLER_NAME_KEY, billerName);
    }

    public String getBillerName() {
        return MDC.get(BILLER_NAME_KEY);
    }

    public void setServiceName(String serviceName) {
        MDC.put(SERVICE_NAME_KEY, serviceName);
    }

    public String getServiceName() {
        return MDC.get(SERVICE_NAME_KEY);
    }

    public void clear() {
        MDC.remove(UUID_KEY);
        MDC.remove(BILLER_NAME_KEY);
        MDC.remove(SERVICE_NAME_KEY);
    }
}
