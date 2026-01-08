package com.az.payment.config.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
public class SummaryLogger {

    private static final Logger SUMMARY_LOG = LoggerFactory.getLogger("SUMMARY_LOGGER");
    private final ObjectMapper objectMapper;
    private final LoggingContext loggingContext;

    public SummaryLogger(LoggingContext loggingContext) {
        this.loggingContext = loggingContext;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void logIncomingRequest(HttpServletRequest request, String body) {
        SummaryLogEntry entry = SummaryLogEntry.builder()
            .timestamp(Instant.now())
            .uuid(loggingContext.getUUID())
            .type("INCOMING_REQUEST")
            .method(request.getMethod())
            .uri(request.getRequestURI())
            .headers(extractHeaders(request))
            .httpStatus(null)
            .body(body)
            .billerName(loggingContext.getBillerName())
            .serviceName(loggingContext.getServiceName())
            .errorRef(null)
            .duration(null)
            .build();

        logEntry(entry);
    }

    public void logIncomingResponse(HttpServletResponse response, String body, long duration) {
        SummaryLogEntry entry = SummaryLogEntry.builder()
            .timestamp(Instant.now())
            .uuid(loggingContext.getUUID())
            .type("INCOMING_RESPONSE")
            .method(null)
            .uri(null)
            .headers(null)
            .httpStatus(response.getStatus())
            .body(body)
            .billerName(loggingContext.getBillerName())
            .serviceName(loggingContext.getServiceName())
            .errorRef(null)
            .duration(duration)
            .build();

        logEntry(entry);
    }

    public void logOutgoingRequest(String url, String method, HttpHeaders headers, String body) {
        SummaryLogEntry entry = SummaryLogEntry.builder()
            .timestamp(Instant.now())
            .uuid(loggingContext.getUUID())
            .type("OUTGOING_REQUEST")
            .method(method)
            .uri(url)
            .headers(extractHeaders(headers))
            .httpStatus(null)
            .body(body)
            .billerName(loggingContext.getBillerName())
            .serviceName(loggingContext.getServiceName())
            .errorRef(null)
            .duration(null)
            .build();

        logEntry(entry);
    }

    public void logOutgoingResponse(String url, int httpStatus, HttpHeaders headers, String body, long duration) {
        SummaryLogEntry entry = SummaryLogEntry.builder()
            .timestamp(Instant.now())
            .uuid(loggingContext.getUUID())
            .type("OUTGOING_RESPONSE")
            .method(null)
            .uri(url)
            .headers(extractHeaders(headers))
            .httpStatus(httpStatus)
            .body(body)
            .billerName(loggingContext.getBillerName())
            .serviceName(loggingContext.getServiceName())
            .errorRef(null)
            .duration(duration)
            .build();

        logEntry(entry);
    }

    public void logError(String errorRef, String method, String uri, int httpStatus) {
        SummaryLogEntry entry = SummaryLogEntry.builder()
            .timestamp(Instant.now())
            .uuid(loggingContext.getUUID())
            .type("ERROR")
            .method(method)
            .uri(uri)
            .headers(null)
            .httpStatus(httpStatus)
            .body(null)
            .billerName(loggingContext.getBillerName())
            .serviceName(loggingContext.getServiceName())
            .errorRef(errorRef)
            .duration(null)
            .build();

        logEntry(entry);
    }

    private void logEntry(SummaryLogEntry entry) {
        try {
            String json = objectMapper.writeValueAsString(entry);
            SUMMARY_LOG.info(json);
        } catch (JsonProcessingException e) {
            SUMMARY_LOG.error("Failed to serialize log entry", e);
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip sensitive headers
            if (!headerName.equalsIgnoreCase("authorization") &&
                !headerName.equalsIgnoreCase("cookie")) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    private Map<String, String> extractHeaders(HttpHeaders httpHeaders) {
        Map<String, String> headers = new HashMap<>();
        if (httpHeaders != null) {
            httpHeaders.forEach((key, value) -> {
                if (!key.equalsIgnoreCase("authorization") &&
                    !key.equalsIgnoreCase("cookie")) {
                    headers.put(key, String.join(",", value));
                }
            });
        }
        return headers;
    }

    @Builder
    @Data
    public static class SummaryLogEntry {
        private Instant timestamp;
        private String uuid;
        private String type;
        private String method;
        private String uri;
        private Map<String, String> headers;
        private Integer httpStatus;
        private String body;
        private String billerName;
        private String serviceName;
        private String errorRef;
        private Long duration;
    }
}
