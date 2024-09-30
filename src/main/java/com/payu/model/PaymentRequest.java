package com.payu.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class PaymentRequest {

    @NotBlank
    private String cardNumber;

    @NotBlank
    private String cardHolderName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private YearMonth expirationDate;

    @NotBlank
    @Pattern(regexp = "\\d{3,4}", message = "El CVV debe tener 3 o 4 dígitos.")
    private String cvv;
}