package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.request.CreateProductRequest;
import com.warehouse.inventory.dto.request.UpdateProductRequest;
import com.warehouse.inventory.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    List<ProductResponse> getAllProducts(String search);

    ProductResponse getProductById(Integer id);

    ProductResponse updateProduct(Integer id, UpdateProductRequest request);
}