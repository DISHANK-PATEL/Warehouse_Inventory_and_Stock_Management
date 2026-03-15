package com.warehouse.inventory.service.impl;

import com.warehouse.inventory.dto.request.CreateProductRequest;
import com.warehouse.inventory.dto.request.UpdateProductRequest;
import com.warehouse.inventory.dto.response.PagedResponse;
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
import com.warehouse.inventory.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
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

        return new ProductResponse(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(
            String search,
            UUID managerId,
            Boolean assigned,
            String breachType,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        User currentUser = getCurrentUser();

        UUID scopedManagerId = (currentUser.getRole() == User.Role.PRODUCT_MANAGER)
                ? currentUser.getId()
                : null;

        Product.BreachStatus breachStatus = parseBreachStatus(breachType);

        Specification<Product> spec = ProductSpecification.withFilters(
                search, managerId, assigned, breachStatus, scopedManagerId
        );

        int clampedSize = Math.min(size, 100);
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, clampedSize, sort);

        Page<ProductResponse> resultPage = productRepository
                .findAll(spec, pageable)
                .map(ProductResponse::new);

        return new PagedResponse<>(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    @CacheEvict(value = "products", key = "#id")
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
    @CacheEvict(value = "products", key = "#id")
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

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getBreachedProducts(
            String breachType,
            UUID managerId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        User currentUser = getCurrentUser();

        UUID scopedManagerId = (currentUser.getRole() == User.Role.PRODUCT_MANAGER)
                ? currentUser.getId()
                : null;

        // Parse optional specific breach type — null means "any breach"
        Product.BreachStatus breachStatus = null;
        if (breachType != null && !breachType.isBlank()) {
            try {
                breachStatus = Product.BreachStatus.valueOf(breachType.toUpperCase());
                if (breachStatus == Product.BreachStatus.NONE) {
                    // Asking for NONE on the /breached endpoint makes no sense
                    throw new IllegalArgumentException(
                            "breachType 'NONE' is not valid for the /breached endpoint. " +
                                    "Use BELOW_MIN or ABOVE_MAX, or omit the parameter to get all breaches.");
                }
            } catch (IllegalArgumentException e) {
                if (e.getMessage().startsWith("breachType")) throw e;
                throw new IllegalArgumentException(
                        "Invalid breachType '" + breachType + "'. Use BELOW_MIN or ABOVE_MAX.");
            }
        }

        int clampedSize = Math.min(size, 100);
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(page, clampedSize, sort);

        Page<ProductResponse> resultPage = productRepository
                .findAll(ProductSpecification.breachedProducts(breachStatus, managerId, scopedManagerId), pageable)
                .map(ProductResponse::new);

        return new PagedResponse<>(resultPage);
    }

    private Product.BreachStatus parseBreachStatus(String breachType) {
        if (breachType == null || breachType.isBlank()) return null;
        try {
            return Product.BreachStatus.valueOf(breachType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid breachType '" + breachType + "'. Must be NONE, BELOW_MIN, or ABOVE_MAX");
        }
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "name"          -> "name";
            case "stockquantity" -> "stockQuantity";
            default              -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}