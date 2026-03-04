package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.CreateProductRequest;
import com.warehouse.inventory.dto.request.UpdateProductRequest;
import com.warehouse.inventory.dto.response.ProductResponse;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ConflictException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

        if (productRepository.existsByName(request.getName())) {
            throw new ConflictException(
                    "A product with the name '" + request.getName() + "' already exists");
        }

        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException(
                    "A product with SKU '" + request.getSku() + "' already exists");
        }

        User currentUser = getCurrentUser();

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .stockQuantity(0)
                .createdBy(currentUser)
                .build();

        return new ProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String search) {

        List<Product> products = (search != null && !search.isBlank())
                ? productRepository.findByNameContainingIgnoreCase(search)
                : productRepository.findAll();

        return products.stream()
                .map(ProductResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        return new ProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer id, UpdateProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        if (productRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new ConflictException(
                    "A product with the name '" + request.getName() + "' already exists");
        }

        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new ConflictException(
                    "A product with SKU '" + request.getSku() + "' already exists");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());

        return new ProductResponse(productRepository.save(product));
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        return userDetails.getUser();
    }
}