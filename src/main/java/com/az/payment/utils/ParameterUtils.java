package com.az.payment.utils;

import com.az.payment.mapper.ParametersMapper;
import com.az.payment.repository.ParameterRepository;
import com.az.payment.response.ParameterResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParameterUtils {
    private final ParameterRepository repository;
    private final ParametersMapper mapper;

    public List<ParameterResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toParameterResponse)
                .collect(Collectors.toList());
    }

    public ParameterResponse findById(long paramId) {
        return repository.findById(paramId)
                .map(mapper::toParameterResponse)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Parameter with id %s not found", paramId)));
    }
}
