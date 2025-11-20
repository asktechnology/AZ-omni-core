package com.az.payment.service;

import com.az.payment.domain.ServiceType;
import com.az.payment.exception.BusinessException;
import com.az.payment.mapper.ServiceMapper;
import com.az.payment.repository.ServiceRepository;
import com.az.payment.request.BillerRequest;
import com.az.payment.request.service.ServiceRequest;
import com.az.payment.response.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.hv.ParameterScriptAssertValidator;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceRepository repository;
    private final ServiceMapper mapper;

    public List<ServiceResponse> findAll() {

        log.info("ServiceService.findAll  return all Active Services");
        return repository
                .findAll()
                .stream()
                .filter(com.az.payment.domain.Service::isActive)
                .map(mapper::toServiceResponse)
                .toList();
    }

    public List<ServiceResponse> findAllInActive() {
        log.info("ServiceService.findAllInActive return all Inactive Services");
        return repository
                .findAll()
                .stream()
                .filter(service -> !service.isActive())
                .map(mapper::toServiceResponse)
                .toList();
    }

    public ServiceResponse findById(long serviceId) {
        log.info("ServiceService.findById  return service with id {}", serviceId);

        return repository.findById(serviceId)
                .map(mapper::toServiceResponse)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Service with id %s not found", serviceId)));
    }

    public ServiceResponse createService(ServiceRequest request) {
        log.info("ServiceService.createService request : {}", request);
        var optionalService = repository.findByName(request.name());
        if (optionalService.isPresent())
            throw new RuntimeException("Service already exists");
        var service = mapper.toService(request);
        log.info("ServiceService.createService mapped request to  service to be created  : {}", service);
        return mapper.toServiceResponse(repository.save(service));
    }

    public ServiceResponse toggleStatus(long serviceId) {
        log.info("ServiceService.toggleStatus request : {}", serviceId);
        var optionalService = repository.findById(serviceId);
        if (optionalService.isEmpty())
            throw new RuntimeException("Service does not exist");
        var service = optionalService.get();
        service.setActive(!service.isActive());
        return mapper.toServiceResponse(repository.save(service));
    }

    public Long deleteService(long serviceId) {
        log.info("ServiceService.deleteService request To delete Service With Id : {}", serviceId);
        var optionalService = repository.findById(serviceId);
        if (optionalService.isEmpty())
            throw new RuntimeException("Service does not exist");
        var service = optionalService.get();
        log.info("ServiceService.deleteService service to be deleted  : {}", service);
        repository.delete(service);
        log.info("ServiceService.deleteService service deleted  with Id : {}", service.getId());
        return service.getId();
    }

    public ServiceResponse updateService(ServiceRequest request) {
        return null;
    }


    public List<ParameterResponse> findParameterByServiceId(Long serviceId,String lang) {

        log.info("ServiceService.findParameterByServiceId  ServiceID : {}", serviceId);
        return repository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(format("Service Not found By Id %d", serviceId)))
                .getParameters()
                .stream().
                filter(parameter -> {

                    log.info("inside stream getting parameterId {} and {} and {} and {}",
                            parameter.getId(),
                            !parameter.isGeneratedTransactionId()
                            , (parameter.getParamType() == 1 || parameter.getParamType() == 2), parameter.getIsFixed() != 1
                    );
                    return !parameter.isGeneratedTransactionId()
                            && (parameter.getParamType() == 1 || parameter.getParamType() == 3)
                            && parameter.getIsFixed() != 1
                            && parameter.getDataSourceId()==-1;//a.salah skip datasource parameter
                })
                .map(parameter ->  mapper.toParameterResponse(parameter,lang)).toList();
    }

    public List<InParameterResponse> findResponseParameterByServiceId(Long serviceId) {

        log.info("ServiceService.findResponseParameterByServiceId  ServiceID : {}", serviceId);
        return repository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(format("Service Not found By Id %d", serviceId)))
                .getParameters()
                .stream().
                filter(parameter -> {
                    return !parameter.isGeneratedTransactionId()
                            && (parameter.getParamType() == 3 || parameter.getParamType() == 2)
                            && parameter.getIsFixed() != 1
                            && parameter.getDataSourceId()==-1;//a.salah skip datasource parameter
                })
                .map(mapper::toInParameterResponse).toList();
    }

}
