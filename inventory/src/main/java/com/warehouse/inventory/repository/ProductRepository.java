package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByName(String name);
    boolean existsBySku(String sku);
    boolean existsByNameAndIdNot(String name, UUID id);
    boolean existsBySkuAndIdNot(String sku, UUID id);

    List<Product> findByNameContainingIgnoreCase(String name);
}