package com.az.payment.controller;


import com.az.payment.request.CategoryRequest;
import com.az.payment.response.BillerResponse;
import com.az.payment.response.CategoryResponse;
import com.az.payment.service.CategoryService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll(HttpServletRequest request) {
        String lang = request.getHeader("language") != null ? request.getHeader("language") : "en";
        return ResponseEntity.ok(service.findAll(lang));
    }

    @GetMapping("/{category-id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable("cateogry-id") Long categoryId,HttpServletRequest request) {
        String lang = request.getHeader("language") != null ? request.getHeader("language") : "en";

        return ResponseEntity.ok(service.findById(categoryId,lang));
    }

    @PostMapping
    public ResponseEntity<Long> createCategory(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(service.createCategory(request));
    }

    @DeleteMapping("/{category-id}")
    public ResponseEntity<Long> deleteCategory(@PathVariable("category-id") Long categoryId) {
        service.deleteCategory(categoryId);
        return ResponseEntity.ok().body(categoryId);
    }

    @PutMapping
    public ResponseEntity<CategoryResponse> updateCategory(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(service.updateCategory(request));

    }

    @GetMapping("/billerByCategoryId/{category-id}")
    public ResponseEntity<List<BillerResponse>> findBillerByCategoryId(@PathVariable("category-id") Long categoryId,HttpServletRequest request) {
        String lang = request.getHeader("language") != null ? request.getHeader("language") : "en";
        return ResponseEntity.ok(service.findBillerByCategoryId(categoryId,lang));
    }

    @GetMapping("/PendingbillerByCategoryId/{category-id}")
    public ResponseEntity<List<BillerResponse>> findPendingBillerByCategoryId(@PathVariable("category-id") Long categoryId) {
        return ResponseEntity.ok(service.findInactiveBillerByCategoryId(categoryId));
    }

}
