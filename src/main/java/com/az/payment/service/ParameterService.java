package com.az.payment.service;

import com.az.payment.mapper.ParametersMapper;
import com.az.payment.repository.ParameterRepository;
import com.az.payment.request.parameter.CreateParameterRequest;
import com.az.payment.response.ParameterResponse;
import com.az.payment.utils.ParameterUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParameterService {

    private final ParameterRepository repository;
    private final ParametersMapper mapper;
    private final ParameterUtils helper;

    public List<ParameterResponse> findAll() {
        log.info("ParameterService::findAll Find all parameters");
        return helper.findAll();//a.salah : moved to helper to avoid dependency loop
    }

    public ParameterResponse findById(long paramId) {
        log.info("ParameterService::findById  Find parameter by id {}", paramId);
        return helper.findById(paramId);//a.salah : moved to helper to avoid dependency loop
    }

    @Transactional
    public Long createParameter(CreateParameterRequest request) {
        log.info("ParameterService::createParameter Create parameter {}", request);
        var parameter = mapper.toParameter(request);
        log.info("ParameterService::createParameter mapping CreateParameterRequest :: {}   into  parameter {}", request, parameter);
        return repository.save(parameter).getId();
    }

    @Transactional
    public Long deleteParameter(long paramId) {
        log.info("ParameterService::deleteParameter Delete parameter by id {}", paramId);
        var parameter = repository.findById(paramId).orElseThrow(() -> new EntityNotFoundException(String.format("Parameter with id %s not found", paramId)));
        repository.delete(parameter);
        return parameter.getId();
    }

    public List<ParameterResponse> findByServiceId(long serviceId) {
        return null;
    }

    public List<ParameterResponse> findParameterByServiceId(long serviceId) {
        log.info("ParameterService::findParameterByServiceId Find parameter by service id {}", serviceId);


        return null;
    }
}
