package com.warehouse.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulkOperationJob")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationJob {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private Status status;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "successful_rows")
    private int successfulRows;

    @Column(name = "failed_rows")
    private int failedRows;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum Status {
        PROCESSING, COMPLETED, FAILED
    }
}
