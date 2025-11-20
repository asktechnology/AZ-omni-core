package com.az.payment.service;

import com.az.payment.domain.Biller;
import com.az.payment.exception.BusinessException;
import com.az.payment.mapper.CategoryMapper;
import com.az.payment.repository.CategoryRepository;
import com.az.payment.request.CategoryRequest;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.CategoryResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryResponse findById(Long categoryId,String lang) {
        log.info("retrieve category by id: {}", categoryId);
        return repository.findById(categoryId)
                .map(category -> mapper.toCategoryResponse(category,lang))
                .orElseThrow(() -> new EntityNotFoundException(format("Category not found for Id %d", categoryId)));
    }

    public List<CategoryResponse> findAll(String lang) {
        log.info("retrieve all categories");
        return repository.findAll()
                .stream()
                .map( category -> mapper.toCategoryResponse(category,lang))
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createCategory(CategoryRequest request) {
        log.info("create category: {}", request);
        // check if category is present
        var optionalCategory = repository.findByName(request.name());
        if (optionalCategory.isPresent()) {
            log.info("category with name {} already exists", request.name());
            throw new BusinessException(format("Category with name %s already exists", request.name()));
        }
        var category = mapper.toCategory(request);
        log.info("CategoryService.createCategory:: category mapped as : {} , from categoryRequest  {}", category, request);
        return repository.save(category).getId();
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("delete category: {}", categoryId);
        var category = repository.findById(categoryId).orElseThrow(() -> new BusinessException(format("Category not found for Id %d", categoryId)));
        log.info("category to be deleted is  : {} with categoryId {} ", category, categoryId);
        repository.deleteById(categoryId);
    }

    @Transactional
    public CategoryResponse updateCategory(CategoryRequest request) {
        log.info("update category: {}", request);
        var category = repository.findById(request.id()).orElseThrow(() -> new EntityNotFoundException(format("Category not found for Id %d", request.id())));
        //noinspection LoggingSimilarMessage
        log.info("CategoryService.updateCategory:: category mapped as : {} , from categoryRequest  {}", category, request);
        return mapper.toCategoryResponse(repository.save(category));
    }

    public List<BillerResponse> findBillerByCategoryId(Long categoryId,String lang) {
        log.info("find biller by id: {}", categoryId);
        var optionalBillers = repository.findById(categoryId).orElseThrow(() -> new EntityNotFoundException(format("Category not found for Id %d", categoryId)))
                .getBillers();
        log.info("after getting all billers with count {} ", optionalBillers.size());
        log.info("maps through and map responses");

        return optionalBillers.stream()
                .filter(Biller::isActive)
                .map(biller-> mapper.toBillerResponse(biller,lang))
                .toList();
    }


    public List<BillerResponse> findInactiveBillerByCategoryId(Long categoryId) {
        log.info("CategoryService::findInactiveBillerByCategoryId find biller by id: {}", categoryId);
        var optionalBillers = repository.findById(categoryId).orElseThrow(() -> new EntityNotFoundException(format("Category not found for Id %d", categoryId)))
                .getBillers();
        log.info("CategoryService::findInactiveBillerByCategoryId after getting all billers with count {} ", optionalBillers.size());

        return optionalBillers.stream()
                .filter(biller -> !biller.isActive())
                .map(mapper::toBillerResponse)
                .toList();
    }
}
