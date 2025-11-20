package com.az.payment.mapper;

import com.az.payment.domain.Biller;
import com.az.payment.domain.Category;
import com.az.payment.request.CategoryRequest;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.CategoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CategoryMapper {


    public CategoryResponse toCategoryResponse(Category category,String lang ) {
        return new CategoryResponse(
                category.getId(),
                lang.equals("en") ? category.getName() : category.getNameAr(),
                category.getDescription(),
                category.getImageUrl()
        );
    }

    public CategoryResponse toCategoryResponse(Category category ) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl()
        );
    }
    public Category toCategory(CategoryRequest request) {
        return Category.builder()
                .id(request.id())
                .name(request.name())
                .imageUrl(request.imageUrl())
                .build();
    }

    public Biller checkIsActive(Biller biller) {
        log.info("biller is not active {}", biller.getId());
        if (biller.isActive()) {
            return biller;
        } else return null;
    }

    public BillerResponse toBillerResponse(Biller biller) {
        return new BillerResponse(
                biller.getId(),
                biller.getName(),
                biller.getDescription(),
                biller.getImageUrl()
        );
    }
    public BillerResponse toBillerResponse(Biller biller,String lang) {
        return new BillerResponse(
                biller.getId(),
                lang.equals("en") ? biller.getName() : biller.getNameAr(),
                biller.getDescription(),
                biller.getImageUrl()
        );
    }
}
