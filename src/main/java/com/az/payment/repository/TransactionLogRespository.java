package com.az.payment.repository;


import com.az.payment.domain.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRespository extends JpaRepository<TransactionLog,Long> {
}
