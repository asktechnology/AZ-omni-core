package com.az.payment.config.logging;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final SummaryLogger summaryLogger;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        long startTime = System.currentTimeMillis();

        // Log outgoing request
        String requestBody = new String(body, StandardCharsets.UTF_8);
        summaryLogger.logOutgoingRequest(
            request.getURI().toString(),
            request.getMethod().name(),
            request.getHeaders(),
            requestBody
        );

        // Execute request
        ClientHttpResponse response = execution.execute(request, body);

        // Buffer response to allow multiple reads
        ClientHttpResponse bufferedResponse = new BufferingClientHttpResponseWrapper(response);

        // Log outgoing response
        String responseBody = StreamUtils.copyToString(bufferedResponse.getBody(), StandardCharsets.UTF_8);
        long duration = System.currentTimeMillis() - startTime;
        summaryLogger.logOutgoingResponse(
            request.getURI().toString(),
            bufferedResponse.getStatusCode().value(),
            bufferedResponse.getHeaders(),
            responseBody,
            duration
        );

        return bufferedResponse;
    }

    // Inner class to buffer response for multiple reads
    private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {
        private final ClientHttpResponse response;
        private byte[] body;

        public BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
            this.response = response;
        }

        @Override
        public org.springframework.http.HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public InputStream getBody() throws IOException {
            if (this.body == null) {
                this.body = StreamUtils.copyToByteArray(response.getBody());
            }
            return new ByteArrayInputStream(this.body);
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return response.getHeaders();
        }
    }
}
