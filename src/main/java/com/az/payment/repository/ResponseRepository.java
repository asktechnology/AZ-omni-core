package com.az.payment.repository;

import com.az.payment.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
    List<Response> findByBillerIdAndExternalResponseCode(Long billerId, String externalResponseCode);

    List<Response> findByBillerIdAndIsSuccess(long billerId, int i);

    List<Response> findByBillerIdAndIsTimeout(long billerId, int i);
}
