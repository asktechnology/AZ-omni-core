package com.az.payment.repository;

import com.az.payment.domain.Biller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BillerRepository extends JpaRepository<Biller, Long> {

    List<Biller> findAllByActive(boolean active);


}

