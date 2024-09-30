package com.payu.controller;

import com.payu.model.PaymentRequest;
import com.payu.model.RefundRequest;
import com.payu.model.Transaction;
import com.payu.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Transaction> makePayment(@Valid @RequestBody PaymentRequest request) {
        Transaction transaction = paymentService.processPayment(request);
        return ResponseEntity.status(201).body(transaction);
    }

    @PostMapping("/refunds")
    public ResponseEntity<Transaction> makeRefund(@Valid @RequestBody RefundRequest request) {
        Transaction transaction = paymentService.processRefund(request);
        return ResponseEntity.status(201).body(transaction);
    }
}