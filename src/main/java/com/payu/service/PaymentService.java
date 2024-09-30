package com.payu.service;

import com.payu.exception.FraudException;
import com.payu.exception.TransactionNotFoundException;
import com.payu.model.BankResponse;
import com.payu.model.PaymentRequest;
import com.payu.model.RefundRequest;
import com.payu.model.Transaction;
import com.payu.model.TransactionType;
import com.payu.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AntiFraudService antiFraudService;

    @Autowired
    private BankService bankService;

    public Transaction processPayment(PaymentRequest request) {
        // Validar información del pagador
        validatePayerInfo(request);

        // Análisis antifraude
        if (antiFraudService.isFraudulent(request)) {
            throw new FraudException("Transacción marcada como fraudulenta por el sistema antifraude.");
        }

        // Procesar pago con el banco
        BankResponse bankResponse = bankService.processPayment(request);

        // Guardar transacción en la base de datos
        Transaction transaction = new Transaction();
        transaction.setCardNumber(request.getCardNumber());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setType(TransactionType.PURCHASE);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setBankTransactionId(bankResponse.getTransactionId());

        return transactionRepository.save(transaction);
    }

    public Transaction processRefund(RefundRequest request) {
        if (request.getOriginalTransactionId() == null) {
            throw new IllegalArgumentException("El ID de la transacción original es obligatorio.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del reembolso debe ser mayor que cero.");
        }

        Optional<Transaction> originalTransactionOpt = transactionRepository.findById(request.getOriginalTransactionId());
        if (!originalTransactionOpt.isPresent()) {
            throw new TransactionNotFoundException("Transacción original no encontrada.");
        }
        Transaction originalTransaction = originalTransactionOpt.get();

        if (originalTransaction.getType() != TransactionType.PURCHASE) {
            throw new IllegalArgumentException("Solo se pueden reembolsar transacciones de compra.");
        }

        BigDecimal totalRefunded = transactionRepository.sumRefundsByOriginalTransactionId(originalTransaction.getId());
        if (totalRefunded == null) {
            totalRefunded = BigDecimal.ZERO;
        }
        BigDecimal availableAmount = originalTransaction.getAmount().subtract(totalRefunded);
        if (request.getAmount().compareTo(availableAmount) > 0) {
            throw new IllegalArgumentException("El monto del reembolso excede el monto disponible.");
        }

        BankResponse bankResponse = bankService.processRefund(request);

        Transaction refundTransaction = new Transaction();
        refundTransaction.setCardNumber(originalTransaction.getCardNumber());
        refundTransaction.setAmount(request.getAmount().negate()); // Monto negativo para indicar reembolso
        refundTransaction.setCurrency(originalTransaction.getCurrency());
        refundTransaction.setType(TransactionType.REFUND);
        refundTransaction.setTimestamp(LocalDateTime.now());
        refundTransaction.setOriginalTransactionId(originalTransaction.getId());
        refundTransaction.setBankTransactionId(bankResponse.getTransactionId());

        return transactionRepository.save(refundTransaction);
    }

    private void validatePayerInfo(PaymentRequest request) {
        if (request.getCardNumber() == null || !isValidCardNumber(request.getCardNumber())) {
            throw new IllegalArgumentException("El número de tarjeta es inválido.");
        }

        if (request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del titular es obligatorio.");
        }

        if (request.getExpirationDate() == null || isCardExpired(request.getExpirationDate())) {
            throw new IllegalArgumentException("La tarjeta ha expirado.");
        }

        if (request.getCvv() == null || !request.getCvv().matches("\\d{3,4}")) {
            throw new IllegalArgumentException("El CVV es inválido.");
        }
    }

    private boolean isValidCardNumber(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private boolean isCardExpired(YearMonth expirationDate) {
        YearMonth currentMonth = YearMonth.now();
        return expirationDate.isBefore(currentMonth);
    }
}
