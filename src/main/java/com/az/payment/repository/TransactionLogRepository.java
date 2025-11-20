package com.az.payment.repository;

import com.az.payment.domain.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog,Long> {
    Optional<TransactionLog> findByTransactionId(String transactionId);
}
