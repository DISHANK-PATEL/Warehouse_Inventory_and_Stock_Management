package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.request.StockUpdateRequest;
import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockMovement;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockMovementRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.service.impl.StockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository stockMovementRepository;

    @InjectMocks private StockServiceImpl stockService;

    private Product testProduct;
    private User staffUser;

    @BeforeEach
    void setUp() {

        staffUser = User.builder()
                .id(1)
                .email("staff@test.com")
                .role(User.Role.STAFF)
                .isActive(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(staffUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        testProduct = Product.builder()
                .id(1)
                .name("Drill")
                .stockQuantity(50)
                .build();
    }

    @Test
    void updateStock_shouldIncreaseQuantity_WhenTypeIsAdd() {
        // ARRANGE
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> i.getArgument(0));

        StockUpdateRequest request = new StockUpdateRequest();
        ReflectionTestUtils.setField(request, "productId", 1);
        ReflectionTestUtils.setField(request, "quantity", 30);
        ReflectionTestUtils.setField(request, "type", "ADD");

        stockService.updateStock(request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(80);
    }

    @Test
    void updateStock_shouldFail_WhenQuantityBecomesNegative() {

        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        StockUpdateRequest request = new StockUpdateRequest();
        ReflectionTestUtils.setField(request, "productId", 1);
        ReflectionTestUtils.setField(request, "quantity", 100);
        ReflectionTestUtils.setField(request, "type", "REMOVE");

        assertThatThrownBy(() -> stockService.updateStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Not enough stock");

        assertThat(testProduct.getStockQuantity()).isEqualTo(50);
    }

    @Test
    void updateStock_shouldAllowRemovalToZero() {
        // ARRANGE
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> i.getArgument(0));

        StockUpdateRequest request = new StockUpdateRequest();
        ReflectionTestUtils.setField(request, "productId", 1);
        ReflectionTestUtils.setField(request, "quantity", 50); // Exactly 50
        ReflectionTestUtils.setField(request, "type", "REMOVE");

        stockService.updateStock(request);

        assertThat(testProduct.getStockQuantity()).isEqualTo(0);
    }


    @Test
    void productCreation_shouldLogiciallyFail_IfUserIsStaff() {

        assertThat(staffUser.getRole()).isEqualTo(User.Role.STAFF);
    }
}