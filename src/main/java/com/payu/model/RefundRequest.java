package com.payu.model;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;

@Data
public class RefundRequest {

    @NotNull
    private Long originalTransactionId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

}