package com.az.payment.repository;

import com.az.payment.domain.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParameterRepository extends JpaRepository<Parameter, Long> {

    //should add filters of type in out / is fixed /
//    List<Parameter> findByServicesAndParamTypeInAndIsFixedIsFalse(List<com.az.payment.domain.Service> service, List<Integer> paramTypes);

//    List<Parameter> findByServicesAndParamTypeInAndIsFixedIsFalseAndIsGeneratedTransactionIdIsFalse(List<com.az.payment.domain.Service> service, List<Integer> paramTypes);


}
