package com.az.payment.config.logging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class ErrorLogger {

    private static final Logger ERROR_LOG = LoggerFactory.getLogger("ERROR_LOGGER");
    private static final AtomicLong ERROR_COUNTER = new AtomicLong(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LoggingContext loggingContext;

    public String logError(Exception e, String method, String uri) {
        String uuid = loggingContext.getUUID();
        String errorRef = generateErrorRef(uuid);

        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(String.format("[%s] [UUID: %s] [%s]\n",
            LocalDateTime.now().format(FORMATTER),
            uuid != null ? uuid : "UNKNOWN",
            errorRef));
        errorMsg.append(String.format("Error Type: %s\n", e.getClass().getSimpleName()));
        errorMsg.append(String.format("Message: %s\n\n", e.getMessage()));

        errorMsg.append("Request Context:\n");
        errorMsg.append(String.format("  Method: %s\n", method != null ? method : "UNKNOWN"));
        errorMsg.append(String.format("  URI: %s\n", uri != null ? uri : "UNKNOWN"));
        errorMsg.append(String.format("  Biller: %s\n", loggingContext.getBillerName() != null ? loggingContext.getBillerName() : "UNKNOWN"));
        errorMsg.append(String.format("  Service: %s\n\n", loggingContext.getServiceName() != null ? loggingContext.getServiceName() : "UNKNOWN"));

        errorMsg.append("Stack Trace (com.az.payment only):\n");
        String filteredStackTrace = filterStackTrace(e);
        if (filteredStackTrace.isEmpty()) {
            errorMsg.append("  (No stack trace from com.az.payment package)\n");
        } else {
            errorMsg.append(filteredStackTrace);
        }
        errorMsg.append("\n---\n\n");

        ERROR_LOG.error(errorMsg.toString());

        return errorRef;
    }

    private String generateErrorRef(String uuid) {
        return String.format("ERR-%s-%03d",
            uuid != null ? uuid : "UNKNOWN",
            ERROR_COUNTER.incrementAndGet() % 1000);
    }

    private String filterStackTrace(Exception e) {
        StringBuilder filtered = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().startsWith("com.az.payment")) {
                filtered.append(String.format("  at %s.%s(%s:%d)\n",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()));
            }
        }
        return filtered.toString();
    }
}
