package com.az.payment.controller;


import com.az.payment.request.parameter.CreateParameterRequest;
import com.az.payment.response.ParameterResponse;
import com.az.payment.service.ParameterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parameter")
@RequiredArgsConstructor
public class ParameterController {

    private final ParameterService service;

    @GetMapping
    public ResponseEntity<List<ParameterResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{param-id}")
    public ResponseEntity<ParameterResponse> findById(@PathVariable("param-id") long paramId) {
        return ResponseEntity.ok(service.findById(paramId));
    }

    @PostMapping
    public ResponseEntity<Long> createParameter(@RequestBody @Valid CreateParameterRequest request) {
        return ResponseEntity.ok(service.createParameter(request));
    }

    @DeleteMapping("/{param-id}")
    public ResponseEntity<Long> deleteParameter(@PathVariable("param-id") long paramId) {
        return ResponseEntity.ok(service.deleteParameter(paramId));
    }

    @GetMapping("/byService/{service-id}")
    public ResponseEntity<List<ParameterResponse>> findByServiceId(@PathVariable("service-id") long serviceId) {
        return ResponseEntity.ok(service.findByServiceId(serviceId));
    }

    @GetMapping("/parametersByBillerId/{service-id}")
    public ResponseEntity<List<ParameterResponse>> findParameterByServiceId(@PathVariable("service-id") long serviceId) {
        return ResponseEntity.ok(service.findParameterByServiceId(serviceId));
    }
}
