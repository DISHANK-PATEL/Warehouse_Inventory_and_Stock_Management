package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.request.CreateProductRequest;
import com.warehouse.inventory.dto.request.UpdateProductRequest;
import com.warehouse.inventory.dto.response.PagedResponse;
import com.warehouse.inventory.dto.response.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    /**
     * GET /products — dynamic multi-criteria filtering with pagination.
     *
     * @param search     Partial name match
     * @param managerId  Filter by assigned PM (Admin/Staff only)
     * @param assigned   false = only unassigned products
     * @param breachType BELOW_MIN | ABOVE_MAX | NONE
     * @param page       Page number (0-based)
     * @param size       Page size (max 100)
     * @param sortBy     name | stockQuantity | createdAt
     * @param sortDir    asc | desc
     */
    PagedResponse<ProductResponse> getAllProducts(
            String search,
            UUID managerId,
            Boolean assigned,
            String breachType,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ProductResponse getProductById(UUID id);

    ProductResponse updateProduct(UUID id, UpdateProductRequest request);
}