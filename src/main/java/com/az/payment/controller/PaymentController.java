package com.az.payment.controller;


import com.az.payment.request.payment.CheckStatusRequest;
import com.az.payment.request.payment.PostCheckRequest;
import com.az.payment.request.payment.ProcessRequest;
import com.az.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;


    @PostMapping("/process")
    public ResponseEntity<?> processPaymentRequest(@RequestBody ProcessRequest request, HttpServletRequest httpServletRequest) {
        String lang = httpServletRequest.getHeader("language") != null ? httpServletRequest.getHeader("language") : "en";

        return ResponseEntity.ok(service.processRequest(request,lang));
    }

    @PostMapping("/postCheck")
    public ResponseEntity<?> postCheck(@RequestBody PostCheckRequest request, HttpServletRequest httpServletRequest) {
        String lang = httpServletRequest.getHeader("language") != null ? httpServletRequest.getHeader("language") : "en";

        return ResponseEntity.ok(service.postCheck(request,lang));
    }

    @PostMapping("/checkStatus")
    public ResponseEntity<?> checkStatus(@RequestBody CheckStatusRequest request, HttpServletRequest httpServletRequest) {
        String lang = httpServletRequest.getHeader("language") != null ? httpServletRequest.getHeader("language") : "en";

        return ResponseEntity.ok(service.checkStatus(request,lang));
    }


}
