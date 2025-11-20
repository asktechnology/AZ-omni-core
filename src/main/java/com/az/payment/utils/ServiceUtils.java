package com.az.payment.utils;

import com.az.payment.domain.Parameter;
import com.az.payment.exception.BusinessException;
import com.az.payment.repository.ServiceRepository;
import com.az.payment.domain.Service;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

@Component
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ServiceUtils {

    private final ServiceRepository serviceRepository;

    public Parameter getSpecialParameter(long serviceId, String paramKey){
        List<Parameter> parameters = getSpecialParameters(serviceId);
        return  getSpecialParameter(parameters, paramKey);
    }

    public Parameter getSpecialParameter(List<Parameter> parameters, String paramKey){

        if(paramKey=="amount")
            return parameters.stream().filter(param -> param.getIsAmount() == 1).findFirst().orElse(new Parameter());
        if(paramKey=="billId")
            return parameters.stream().filter(param -> param.getIsBillid() == 1).findFirst().orElse(new Parameter());
        if(paramKey=="resCode")
            return parameters.stream().filter(param -> param.getIsBillRescode() == 1).findFirst().orElse(new Parameter());
        if(paramKey=="resMsg")
            return parameters.stream().filter(param -> param.getIsBillResmsg() == 1).findFirst().orElse(new Parameter());
        if(paramKey=="extRrn")
            return parameters.stream().filter(param -> param.getIsExtrrn() == 1).findFirst().orElse(new Parameter());
        return  null;
    }

    public List<Parameter> getSpecialParameters(long serviceId){
        List<Parameter> parameters = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(format("Service Not found By Id %d", serviceId)))
                .getParameters()
                .stream().
                filter(parameter -> {
//                    return !parameter.isGeneratedTransactionId() && (parameter.getParamType() == 1 || parameter.getParamType() == 3) && parameter.getIsFixed() != 1;
                    return parameter.getIsAmount() == 1||
                            parameter.getIsBillid() == 1||
                            parameter.getIsBillRescode() == 1||
                            parameter.getIsBillResmsg() == 1||
                            parameter.getIsExtrrn() == 1;
                }).toList();
        return parameters;
    }

    public long getParameterIdByExtKey(long serviceId, String extKey){
        long id = -1;

        Parameter parameter = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(format("Service Not found By Id %d", serviceId)))
                .getParameters()
                .stream().
                filter(param -> {return extKey.equals(param.getExternalKey());}).findFirst().orElse(Parameter.builder().id(-Math.abs(extKey.hashCode())).build());//return default parameter with negative unique ID

        id = parameter.getId();
        return id;
    }

    public Stream<Parameter> getNonAdditionalInParameters(long serviceId){
        return getNonAdditionalInParameters(serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("Service not found : "+serviceId)).getParameters());
    }

    public Stream<Parameter> getNonAdditionalInParameters(List<Parameter> parameters){
        return parameters.stream().filter(parameter -> isNonAdditionalInParameter(parameter));
    }

    public boolean isNonAdditionalInParameter(Parameter parameter){
        return isInParameter(parameter) && !isAdditionalParameter(parameter);
    }

    public Stream<Parameter> getAdditionalInParameters(long serviceId){
        return getAdditionalInParameters(serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("Service not found : "+serviceId)).getParameters());
    }

    public Stream<Parameter> getAdditionalInParameters(List<Parameter> parameters){
        return parameters.stream().filter(parameter -> isAdditionalInParameter(parameter));
    }

    public Stream<Parameter> getDataSourceParameters(long serviceId){
        return getDataSourceParameters(serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException("Service not found : "+serviceId)).getParameters());
    }

    public Stream<Parameter> getDataSourceParameters(List<Parameter> parameters){
        return parameters.stream().filter(parameter -> isInParameter(parameter) && isDataSourceParameter(parameter));
    }

    public boolean isAdditionalInParameter(Parameter parameter){
        return isInParameter(parameter) && isAdditionalParameter(parameter);
    }

    public boolean isInParameter(Parameter parameter){
        return parameter.getParamType() == 1 || parameter.getParamType() == 3;
    }

    public boolean isOutParameter(Parameter parameter){
        return parameter.getParamType() == 2 || parameter.getParamType() == 3;
    }

    public boolean isAdditionalParameter(Parameter parameter){
        return isFixedParameter(parameter) || isGeneratedIdParameter(parameter)
                || isDataSourceParameter(parameter);
    }

    public boolean isFixedParameter(Parameter parameter){
        return parameter.getIsFixed() == 1;
    }

    public boolean isGeneratedIdParameter(Parameter parameter){
        return parameter.isGeneratedTransactionId();
    }

    public boolean isDataSourceParameter(Parameter parameter){
        return !(parameter.getDataSourceId()==-1);
    }

    public Service getCheckStatusService(long serviceId){
        Service service = new Service();
        System.out.println("serviceId : "+serviceId);

        Service mainService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(format("Service Not found By Id %d", serviceId)));
        System.out.println("mainService : "+mainService);

        if(mainService.getAfterServiceId()==0)
            service.setId(0);
        else
            service = serviceRepository.findById(mainService.getAfterServiceId())
                    .orElseThrow(() -> new EntityNotFoundException(format("After Service Not found for main Service(:%d)", serviceId)));

        return service;
    }
}
