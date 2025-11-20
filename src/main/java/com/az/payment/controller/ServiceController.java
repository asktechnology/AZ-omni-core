package com.az.payment.controller;

import com.az.payment.request.BillerRequest;
import com.az.payment.request.service.ServiceRequest;
import com.az.payment.response.ApiResponse;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.ParameterResponse;
import com.az.payment.response.ServiceResponse;
import com.az.payment.service.ServiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/service")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService service;


    @GetMapping
    public ResponseEntity<List<ServiceResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/pendingServices")
    public ResponseEntity<List<ServiceResponse>> findInActiveService() {
        return ResponseEntity.ok(service.findAllInActive());
    }

    @GetMapping("/{service-id}")
    public ResponseEntity<ServiceResponse> findById(@PathVariable("service-id") long serviceId) {
        return ResponseEntity.ok(service.findById(serviceId));
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(@RequestBody @Valid ServiceRequest request) {
        return ResponseEntity.ok(service.createService(request));
    }

    @PutMapping
    public ResponseEntity<ServiceResponse> updateService(@RequestBody @Valid ServiceRequest request) {
        return ResponseEntity.ok(service.updateService(request));
    }

    @PutMapping("/toggleStatus/{service-id}")
    public ResponseEntity<ServiceResponse> toggleStatus(@PathVariable("service-id") long serviceId) {
        return ResponseEntity.ok(service.toggleStatus(serviceId));
    }

    @DeleteMapping("{service-id}")
    public ResponseEntity<Long> deleteService(@PathVariable("service-id") long serviceId) {
        return ResponseEntity.ok(service.deleteService(serviceId));
    }

    @GetMapping("/parameterByServiceId/{service-id}")
    public ResponseEntity<List<ParameterResponse>> findBillerByCategoryId(@PathVariable("service-id") Long serviceId, HttpServletRequest request) {
        String lang = request.getHeader("language") != null ? request.getHeader("language") : "en";

        return ResponseEntity.ok(service.findParameterByServiceId(serviceId,lang));
    }

}
