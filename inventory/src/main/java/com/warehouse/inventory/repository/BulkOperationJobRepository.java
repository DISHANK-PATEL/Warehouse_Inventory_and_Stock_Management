package com.warehouse.inventory.repository;

import com.warehouse.inventory.entity.BulkOperationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkOperationJobRepository extends JpaRepository<BulkOperationJob, UUID> {

    List<BulkOperationJob> findAllByOrderBySubmittedAtDesc();

    List<BulkOperationJob> findBySubmittedByIdOrderBySubmittedAtDesc(UUID submittedById);

}