package com.az.payment.service;

import com.az.payment.domain.ServiceType;
import com.az.payment.exception.BusinessException;
import com.az.payment.mapper.BillerMapper;
import com.az.payment.repository.BillerRepository;
import com.az.payment.request.BillerRequest;
import com.az.payment.response.ApiResponse;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.ServiceResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillerService {

    public static final String BILLER_NOT_FOUND_FOR_ID_D = "Biller not found for Id %d";
    private final BillerRepository repository;
    private final BillerMapper mapper;

    @Cacheable(value = "biller")
    public ApiResponse findAll() {
        log.info("BillerService.findAll:: Find all biller ");
        return new ApiResponse(repository.findAllByActive(true)
                .stream()
                .map(mapper::toBillerResponse)
                .collect(Collectors.toList()));
    }

    @Cacheable(value = "biller", key = "#billerId", condition = "#billerId>=0")
    public ApiResponse findById(long billerId) {
        log.info(format("BillerService.findById:: Find Biller with id : %d", billerId));
        return new ApiResponse(
                repository.findById(billerId)
                        .map(mapper::toBillerResponse)
                        .orElseThrow(() -> new EntityNotFoundException(String.format(BILLER_NOT_FOUND_FOR_ID_D, billerId))));

    }


    @Transactional
    public ApiResponse createBiller(BillerRequest request) {
        log.info("BillerService.createBiller:: Create Biller: {}", request);
        var biller = mapper.toBiller(request);
        log.info("BillerService.createBiller:: mapping billerRequest : {} , into biller {}", request, biller);
        return new ApiResponse(mapper.toBillerResponse((repository.save(biller))));
    }

    @Transactional
    public ApiResponse updateBiller(BillerRequest request) {
        log.info("BillerService.updateBiller:: Update Biller: {}", request);
        var biller = repository.findById(request.id()).orElseThrow(() -> new EntityNotFoundException(format(BILLER_NOT_FOUND_FOR_ID_D, request.id())));
        log.info("BillerService.updateBiller::Biller with id %d {} found : {}", request.id(), request);
        biller = mapper.toBiller(request);
        log.info("BillerService.updateBiller:: Updating biller : {}", biller);
        return new ApiResponse(mapper.toBillerResponse(repository.save(biller)));
    }

    @Transactional
    public ApiResponse toggleStatus(long billerId) {
        log.info("BillerService.toggleStatus:: Toggle Biller status with id {} ", billerId);
        var biller = repository.findById(billerId).orElseThrow(() -> new EntityNotFoundException(format(BILLER_NOT_FOUND_FOR_ID_D, billerId)));
        log.info("BillerService.toggleStatus:: Updating biller  status from  : {} : into  {} ", biller.isActive(), !biller.isActive());
        biller.setActive(!biller.isActive());
        return new ApiResponse(mapper.toBillerResponse(repository.save(biller)));
    }

    @Transactional
    @Caching(
            evict = {@CacheEvict(value = "biller", allEntries = true), @CacheEvict(value = "biller", key = "#billerId")
            })
    public Long deleteBiller(long billerId) {
        log.info("BillerService::deleteBiller  Delete Biller with id {} ", billerId);
        var biller = repository.findById(billerId).orElseThrow(() -> new EntityNotFoundException(format(BILLER_NOT_FOUND_FOR_ID_D, billerId)));
        if (biller.getCategories().isEmpty()) {
            log.info("BillerService::deleteBiller:: Deleting Biller with id {} ", billerId);
            repository.delete(biller);
            return billerId;
        } else {
            log.info("Can not delete biller with id {} because there are categories with count {} ", billerId, biller.getCategories().size());
            throw new BusinessException("Can not delete Biller with category defined for Id " + billerId);
        }
    }

    public List<BillerResponse> findAllInActive() {
        log.info("BillerService.findAllInActive:: Find all biller with status {} ", false);
        return repository.findAllByActive(false)
                .stream()
                .map(mapper::toBillerResponse)
                .collect(Collectors.toList());
    }

    public List<ServiceResponse> findServiceByBillerId(long billerId,String lang) {

        log.info("BillerService::findServiceByBillerId:: Find Biller with id {}", billerId);
        return repository.findById(billerId)
                .orElseThrow(() -> new EntityNotFoundException(format(BILLER_NOT_FOUND_FOR_ID_D, billerId)))
                .getServices()
                .stream().
                filter(service -> {
                    log.info("inside stream getting serviceID {}", service.getId());
                    return service.isActive() && (service.getServiceType() == ServiceType.INQUIRY || service.getServiceType() == ServiceType.BOTH);
                })
                .map(service ->  mapper.toServiceResponse(service,lang)).toList();
    }
}
