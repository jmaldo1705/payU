package com.payu.repository;

import com.payu.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.originalTransactionId = :originalTransactionId AND t.type = com.payu.model.TransactionType.REFUND")
    BigDecimal sumRefundsByOriginalTransactionId(@Param("originalTransactionId") Long originalTransactionId);
}
