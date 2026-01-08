package com.az.payment.config.logging;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final SummaryLogger summaryLogger;
    private final LoggingContext loggingContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip for non-API endpoints
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate UUID for this request
        loggingContext.generateAndSetUUID();

        // Wrap request/response for multiple reads
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Process request first
            filterChain.doFilter(requestWrapper, responseWrapper);

            // After processing, read and log
            String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            summaryLogger.logIncomingRequest(requestWrapper, requestBody);

            // Log incoming response
            String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            long duration = System.currentTimeMillis() - startTime;
            summaryLogger.logIncomingResponse(responseWrapper, responseBody, duration);

        } finally {
            responseWrapper.copyBodyToResponse();
            loggingContext.clear();
        }
    }
}
