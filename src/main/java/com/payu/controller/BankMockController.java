package com.payu.controller;

import com.payu.model.BankResponse;
import com.payu.model.PaymentRequest;
import com.payu.model.RefundRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/mock/bank")
public class BankMockController {

    @PostMapping("/payments")
    public BankResponse processPayment(@RequestBody PaymentRequest request) {
        BankResponse response = new BankResponse();

        if (request.getCardNumber().endsWith("0000")) {
            response.setStatus("DECLINED");
            response.setTransactionId(UUID.randomUUID().toString());
            response.setMessage("Transacción rechazada por el banco.");
        } else {
            response.setStatus("APPROVED");
            response.setTransactionId(UUID.randomUUID().toString());
            response.setMessage("Transacción aprobada.");
        }
        return response;
    }

    @PostMapping("/refunds")
    public BankResponse processRefund(@RequestBody RefundRequest request) {
        BankResponse response = new BankResponse();
        response.setStatus("REFUNDED");
        response.setTransactionId(UUID.randomUUID().toString());
        response.setMessage("Reembolso procesado exitosamente.");
        return response;
    }
}
