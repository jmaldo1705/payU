package com.payu.controller;

import com.payu.model.AntiFraudResponse;
import com.payu.model.PaymentRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/mock/antifraud")
public class AntiFraudMockController {

    @PostMapping("/check")
    public AntiFraudResponse check(@RequestBody PaymentRequest request) {
        AntiFraudResponse response = new AntiFraudResponse();
        response.setFraudulent(request.getAmount().compareTo(new BigDecimal("1000")) > 0);

        return response;
    }
}
