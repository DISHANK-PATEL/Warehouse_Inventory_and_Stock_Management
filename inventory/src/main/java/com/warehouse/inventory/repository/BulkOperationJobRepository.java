package com.warehouse.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkOperationJobRepository extends JpaRepository<BulkOperationJobRepository, UUID> {
}
