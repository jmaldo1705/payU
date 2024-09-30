package com.payu.service;

import com.payu.model.AntiFraudResponse;
import com.payu.model.PaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AntiFraudService {

    private final WebClient webClient;

    public AntiFraudService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }

    public boolean isFraudulent(PaymentRequest request) {
        Mono<AntiFraudResponse> responseMono = webClient.post()
                .uri("/api/mock/antifraud/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AntiFraudResponse.class);

        AntiFraudResponse response = responseMono.block();

        return response != null && response.isFraudulent();
    }
}