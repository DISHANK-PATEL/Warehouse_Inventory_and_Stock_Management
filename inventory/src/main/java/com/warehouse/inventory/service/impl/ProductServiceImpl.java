package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.CreateProductRequest;
import com.warehouse.inventory.dto.request.UpdateProductRequest;
import com.warehouse.inventory.dto.response.ProductResponse;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ConflictException;
import com.warehouse.inventory.exception.ForbiddenException;
import com.warehouse.inventory.exception.ResourceNotFoundException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

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

        User productManager;
        if (request.getProductManagerId() != null) {
            productManager = userRepository.findById(request.getProductManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product Manager not found with id: " + request.getProductManagerId()
                    ));
            if (productManager.getRole() != User.Role.PRODUCT_MANAGER) {
                throw new IllegalArgumentException(
                        "Specified user is not a Product Manager"
                );
            }
        }
        else {
            if (currentUser.getRole() != User.Role.PRODUCT_MANAGER) {
                throw new IllegalArgumentException(
                        "productManagerId is required - creator is not a Product Manager"
                );
            }
            productManager = currentUser;
        }
         if (request.getMinThreshold() != null && request.getMaxThreshold() != null) {
             if (request.getMinThreshold() > request.getMaxThreshold()) {
                 throw new IllegalArgumentException(
                         "minThreshold cannot exceed maxThreshold"
                 );
             }
         }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .sku(request.getSku())
                .stockQuantity(0)
                .reservedQuantity(0)
                .breachStatus(Product.BreachStatus.NONE)
                .productManager(productManager)
                .minThreshold(request.getMinThreshold())
                .maxThreshold(request.getMaxThreshold())
                .createdBy(currentUser)
                .build();

        return new ProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String search) {

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            return productRepository.findByProductManagerId(currentUser.getId())
                    .stream()
                    .map(ProductResponse::new)
                    .toList();
        }

        List<Product> products = (search != null && !search.isBlank())
                ? productRepository.findByNameContainingIgnoreCase(search)
                : productRepository.findAll();

        return products.stream()
                .map(ProductResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.PRODUCT_MANAGER) {
            boolean isAssigned = product.getProductManager() != null &&
                    product.getProductManager().getId().equals(currentUser.getId());

            if (!isAssigned) {
                throw new ForbiddenException("You can only access products assigned to you");
            }
        }

        return new ProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        if (request.getName() != null && productRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new ConflictException(
                    "A product with the name '" + request.getName() + "' already exists"
            );
        }

        if (request.getName() != null && productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new ConflictException(
                    "A product with SKU '" + request.getSku() + "' already exists");
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getSku() != null) product.setSku(request.getSku());

        if (request.getProductManagerId() != null) {
            User newPm = userRepository.findById(request.getProductManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + request.getProductManagerId()
                    ));
            if (newPm.getRole() != User.Role.PRODUCT_MANAGER) {
                throw new IllegalArgumentException("Assigned user is not a Product Manager");
            }

            product.setProductManager(newPm);
        }

        if (request.getMinThreshold() != null) product.setMinThreshold(request.getMinThreshold());
        if (request.getMaxThreshold() != null) product.setMaxThreshold(request.getMaxThreshold());

        if (product.getMinThreshold() != null && product.getMaxThreshold() != null) {
            if (product.getMinThreshold() > product.getMaxThreshold()) {
                throw new IllegalArgumentException("minThreshold cannot exceed maxThreshold");
            }
        }

        return new ProductResponse(productRepository.save(product));
    }

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }
}