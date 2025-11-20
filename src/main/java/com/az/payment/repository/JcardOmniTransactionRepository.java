package com.az.payment.repository;

import com.az.payment.domain.JcardOmniTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JcardOmniTransactionRepository extends JpaRepository<JcardOmniTransaction, Long> {
    Optional<JcardOmniTransaction> findByRrn(String rrn);
}
