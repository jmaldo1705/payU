package com.payu.service;

import com.payu.exception.BankDeclinedException;
import com.payu.model.BankResponse;
import com.payu.model.PaymentRequest;
import com.payu.model.RefundRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
public class BankService {

    private final WebClient webClient;

    public BankService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    public BankResponse processPayment(PaymentRequest request) {
        Mono<BankResponse> responseMono = webClient.post()
                .uri("/api/mock/bank/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BankResponse.class);

        BankResponse response = responseMono.block();

        if (response != null && "APPROVED".equals(response.getStatus())) {
            return response;
        } else {
            throw new BankDeclinedException(response != null ? response.getMessage() : "Error al procesar el pago.");
        }
    }

    public BankResponse processRefund(RefundRequest request) {
        Mono<BankResponse> responseMono = webClient.post()
                .uri("/api/mock/bank/refunds")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BankResponse.class);

        BankResponse response = responseMono.block();

        if (response != null && "REFUNDED".equals(response.getStatus())) {
            return response;
        } else {
            throw new BankDeclinedException(response != null ? response.getMessage() : "Error al procesar el reembolso.");
        }
    }
}