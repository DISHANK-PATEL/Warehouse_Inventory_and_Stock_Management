package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.request.CreateProductRequest;
import com.warehouse.inventory.dto.request.UpdateProductRequest;
import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.ProductResponse;
import com.warehouse.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.createProduct(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProducts(

            // Filtering params
            @RequestParam(required = false)               String  search,
            @RequestParam(required = false)               UUID    managerId,
            @RequestParam(required = false)               Boolean assigned,
            @RequestParam(required = false)               String  breachType,

            // Pagination params — with spec defaults
            @RequestParam(defaultValue = "0")             int     page,
            @RequestParam(defaultValue = "20")            int     size,

            // Sorting params
            @RequestParam(defaultValue = "createdAt")     String  sortBy,
            @RequestParam(defaultValue = "desc")          String  sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProducts(
                        search, managerId, assigned, breachType,
                        page, size, sortBy, sortDir
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.updateProduct(id, request)));
    }
}