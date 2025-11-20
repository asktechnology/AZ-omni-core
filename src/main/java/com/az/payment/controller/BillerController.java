package com.az.payment.controller;

//import com.az.payment.constants.Code;

import com.az.payment.request.BillerRequest;
import com.az.payment.response.ApiResponse;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.ServiceResponse;
import com.az.payment.service.BillerService;
//import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/biller")
@RequiredArgsConstructor
//@Hidden
public class BillerController {

    private final BillerService service;

    @GetMapping
    public ResponseEntity<ApiResponse> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/pendingBiller")
    public ResponseEntity<List<BillerResponse>> findInActiveBiller() {
        return ResponseEntity.ok(service.findAllInActive());
    }

    @GetMapping("/{biller-id}")
    public ResponseEntity<ApiResponse> findById(@PathVariable("biller-id") long billerId) {
        return ResponseEntity.ok(service.findById(billerId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createBiller(@RequestBody @Valid BillerRequest request) {
        return ResponseEntity.ok(service.createBiller(request));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> updateBiller(@RequestBody @Valid BillerRequest request) {
        return ResponseEntity.ok(service.updateBiller(request));
    }

    @PutMapping("/toggleStatus/{biller-id}")
    public ResponseEntity<ApiResponse> toggleStatus(@PathVariable("biller-id") long billerId) {
        return ResponseEntity.ok(service.toggleStatus(billerId));
    }

    @DeleteMapping("{biller-id}")
    public ResponseEntity<Long> deleteBiller(@PathVariable("biller-id") long billerId) {
        return ResponseEntity.ok(service.deleteBiller(billerId));
    }

    @GetMapping("/servicesByBillerId/{biller-id}")
    public ResponseEntity<List<ServiceResponse>> findServicesByBillerId(@PathVariable("biller-id") long billerId, HttpServletRequest request) {
        String lang = request.getHeader("language") != null ? request.getHeader("language") : "en";
        return ResponseEntity.ok(service.findServiceByBillerId(billerId,lang));
    }

}
