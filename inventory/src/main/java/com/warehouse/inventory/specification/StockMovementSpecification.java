package com.warehouse.inventory.specification;

import com.warehouse.inventory.entity.StockMovement;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic multi-criteria filtering for stock movements.
 *
 * Filters:
 *   - productId        exact match on product
 *   - movementType     ADD | REMOVE | RESERVE | RELEASE
 *   - performedById    exact match on user who performed the movement
 *   - startDate        movements on or after
 *   - endDate          movements on or before
 *   - scopedManagerId  when caller is PM, restrict to their assigned products only
 */
public class StockMovementSpecification {

    private StockMovementSpecification() {}

    public static Specification<StockMovement> withFilters(
            UUID productId,
            StockMovement.MovementType movementType,
            UUID performedById,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID scopedManagerId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // PM scope — always applied when caller is PRODUCT_MANAGER
            if (scopedManagerId != null) {
                predicates.add(
                        cb.equal(root.get("product").get("productManager").get("id"), scopedManagerId)
                );
            }

            // Filter by specific product
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }

            // Filter by movement type (ADD, REMOVE, RESERVE, RELEASE)
            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }

            // Filter by who performed the movement
            if (performedById != null) {
                predicates.add(cb.equal(root.get("performedBy").get("id"), performedById));
            }

            // Date range
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
