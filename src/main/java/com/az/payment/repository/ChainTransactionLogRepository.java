package com.az.payment.repository;

 import com.az.payment.domain.ChainTransactionLog;
 import org.springframework.data.jpa.repository.JpaRepository;

public interface ChainTransactionLogRepository extends JpaRepository<ChainTransactionLog,Long> {
}
