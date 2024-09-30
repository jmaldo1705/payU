package com.payu;

import com.payu.exception.BankDeclinedException;
import com.payu.exception.FraudException;
import com.payu.exception.TransactionNotFoundException;
import com.payu.model.BankResponse;
import com.payu.model.PaymentRequest;
import com.payu.model.RefundRequest;
import com.payu.model.Transaction;
import com.payu.model.TransactionType;
import com.payu.repository.TransactionRepository;
import com.payu.service.AntiFraudService;
import com.payu.service.BankService;
import com.payu.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AntiFraudService antiFraudService;

    @Mock
    private BankService bankService;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest validPaymentRequest;

    private RefundRequest validRefundRequest;

    @BeforeEach
    public void setUp() {
        validPaymentRequest = new PaymentRequest();
        validPaymentRequest.setCardNumber("4111111111111111");
        validPaymentRequest.setCardHolderName("Juan Pérez");
        validPaymentRequest.setAmount(new BigDecimal("500.00"));
        validPaymentRequest.setCurrency("USD");
        validPaymentRequest.setExpirationDate(YearMonth.of(2025, 12));
        validPaymentRequest.setCvv("123");

        validRefundRequest = new RefundRequest();
        validRefundRequest.setOriginalTransactionId(1L);
        validRefundRequest.setAmount(new BigDecimal("200.00"));
    }

    @Test
    public void testProcessPayment_Success() {
        // Configurar el comportamiento simulado
        when(antiFraudService.isFraudulent(validPaymentRequest)).thenReturn(false);

        BankResponse bankResponse = new BankResponse();
        bankResponse.setStatus("APPROVED");
        bankResponse.setTransactionId("bank-tx-123");
        when(bankService.processPayment(validPaymentRequest)).thenReturn(bankResponse);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        savedTransaction.setCardNumber(validPaymentRequest.getCardNumber());
        savedTransaction.setAmount(validPaymentRequest.getAmount());
        savedTransaction.setCurrency(validPaymentRequest.getCurrency());
        savedTransaction.setType(TransactionType.PURCHASE);
        savedTransaction.setTimestamp(LocalDateTime.now());
        savedTransaction.setBankTransactionId(bankResponse.getTransactionId());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Ejecutar el método a probar
        Transaction result = paymentService.processPayment(validPaymentRequest);

        // Verificar los resultados
        assertNotNull(result);
        assertEquals(savedTransaction.getId(), result.getId());
        assertEquals(TransactionType.PURCHASE, result.getType());
        assertEquals(validPaymentRequest.getAmount(), result.getAmount());
        assertEquals("bank-tx-123", result.getBankTransactionId());

        // Verificar que los métodos simulados fueron llamados
        verify(antiFraudService).isFraudulent(validPaymentRequest);
        verify(bankService).processPayment(validPaymentRequest);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    public void testProcessPayment_FraudulentTransaction() {
        // Configurar el comportamiento simulado
        when(antiFraudService.isFraudulent(validPaymentRequest)).thenReturn(true);

        // Ejecutar y verificar que se lanza la excepción FraudException
        FraudException exception = assertThrows(FraudException.class, () -> {
            paymentService.processPayment(validPaymentRequest);
        });

        assertEquals("Transacción marcada como fraudulenta por el sistema antifraude.", exception.getMessage());

        // Verificar que el servicio bancario no fue llamado
        verify(bankService, never()).processPayment(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testProcessPayment_BankDeclined() {
        // Configurar el comportamiento simulado
        when(antiFraudService.isFraudulent(validPaymentRequest)).thenReturn(false);

        when(bankService.processPayment(validPaymentRequest)).thenThrow(new BankDeclinedException("Transacción rechazada por el banco."));

        // Ejecutar y verificar que se lanza la excepción BankDeclinedException
        BankDeclinedException exception = assertThrows(BankDeclinedException.class, () -> {
            paymentService.processPayment(validPaymentRequest);
        });

        assertEquals("Transacción rechazada por el banco.", exception.getMessage());

        // Verificar que la transacción no fue guardada
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testProcessRefund_Success() {
        // Configurar la transacción original
        Transaction originalTransaction = new Transaction();
        originalTransaction.setId(1L);
        originalTransaction.setAmount(new BigDecimal("500.00"));
        originalTransaction.setCurrency("USD");
        originalTransaction.setType(TransactionType.PURCHASE);
        originalTransaction.setCardNumber("4111111111111111");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(originalTransaction));

        when(transactionRepository.sumRefundsByOriginalTransactionId(1L)).thenReturn(new BigDecimal("100.00"));

        BankResponse bankResponse = new BankResponse();
        bankResponse.setStatus("REFUNDED");
        bankResponse.setTransactionId("bank-tx-refund-123");
        when(bankService.processRefund(validRefundRequest)).thenReturn(bankResponse);

        Transaction refundTransaction = new Transaction();
        refundTransaction.setId(2L);
        refundTransaction.setAmount(validRefundRequest.getAmount().negate());
        refundTransaction.setCurrency(originalTransaction.getCurrency());
        refundTransaction.setType(TransactionType.REFUND);
        refundTransaction.setOriginalTransactionId(originalTransaction.getId());
        refundTransaction.setBankTransactionId(bankResponse.getTransactionId());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(refundTransaction);

        // Ejecutar el método a probar
        Transaction result = paymentService.processRefund(validRefundRequest);

        // Verificar los resultados
        assertNotNull(result);
        assertEquals(TransactionType.REFUND, result.getType());
        assertEquals(validRefundRequest.getAmount().negate(), result.getAmount());
        assertEquals("bank-tx-refund-123", result.getBankTransactionId());

        // Verificar que los métodos simulados fueron llamados
        verify(bankService).processRefund(validRefundRequest);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    public void testProcessRefund_OriginalTransactionNotFound() {
        // Configurar el comportamiento simulado
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        // Ejecutar y verificar que se lanza la excepción TransactionNotFoundException
        TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class, () -> {
            paymentService.processRefund(validRefundRequest);
        });

        assertEquals("Transacción original no encontrada.", exception.getMessage());

        // Verificar que no se llamaron otros métodos
        verify(bankService, never()).processRefund(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testProcessRefund_AmountExceedsAvailable() {
        // Configurar la transacción original
        Transaction originalTransaction = new Transaction();
        originalTransaction.setId(1L);
        originalTransaction.setAmount(new BigDecimal("500.00"));
        originalTransaction.setCurrency("USD");
        originalTransaction.setType(TransactionType.PURCHASE);
        originalTransaction.setCardNumber("4111111111111111");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(originalTransaction));

        when(transactionRepository.sumRefundsByOriginalTransactionId(1L)).thenReturn(new BigDecimal("300.00"));

        // Ajustar el monto del reembolso para exceder el disponible
        validRefundRequest.setAmount(new BigDecimal("250.00"));

        // Ejecutar y verificar que se lanza la excepción IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processRefund(validRefundRequest);
        });

        assertEquals("El monto del reembolso excede el monto disponible.", exception.getMessage());

        // Verificar que no se llamaron otros métodos
        verify(bankService, never()).processRefund(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testProcessPayment_InvalidCardNumber() {
        // Ajustar el número de tarjeta a uno inválido
        validPaymentRequest.setCardNumber("1234567890123456");

        // Ejecutar y verificar que se lanza la excepción IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(validPaymentRequest);
        });

        assertEquals("El número de tarjeta es inválido.", exception.getMessage());

        // Verificar que no se llamaron otros métodos
        verify(antiFraudService, never()).isFraudulent(any());
        verify(bankService, never()).processPayment(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testProcessPayment_CardExpired() {
        // Ajustar la fecha de expiración a una fecha pasada
        validPaymentRequest.setExpirationDate(YearMonth.of(2020, 12));

        // Ejecutar y verificar que se lanza la excepción IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(validPaymentRequest);
        });

        assertEquals("La tarjeta ha expirado.", exception.getMessage());

        // Verificar que no se llamaron otros métodos
        verify(antiFraudService, never()).isFraudulent(any());
        verify(bankService, never()).processPayment(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    public void testProcessPayment_InvalidCvv() {
        // Ajustar el CVV a uno inválido
        validPaymentRequest.setCvv("12");

        // Ejecutar y verificar que se lanza la excepción IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(validPaymentRequest);
        });

        assertEquals("El CVV es inválido.", exception.getMessage());

        // Verificar que no se llamaron otros métodos
        verify(antiFraudService, never()).isFraudulent(any());
        verify(bankService, never()).processPayment(any());
        verify(transactionRepository, never()).save(any());
    }
}