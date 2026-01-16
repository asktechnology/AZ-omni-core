package com.az.payment.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@CrossOrigin(origins = "*") // ✅ allow all origins
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
