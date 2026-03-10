package com.warehouse.inventory.specification;

import com.warehouse.inventory.entity.StockAlert;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic multi-criteria filtering for stock alerts.
 *
 * Filters:
 *   - productId        exact match on product
 *   - breachType       BELOW_MIN | ABOVE_MAX
 *   - startDate        alerts created on or after
 *   - endDate          alerts created on or before
 *   - scopedManagerId  when caller is PM, restrict to their assigned products only
 */
public class StockAlertSpecification {

    private StockAlertSpecification() {}

    public static Specification<StockAlert> withFilters(
            UUID productId,
            StockAlert.BreachType breachType,
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

            // Filter by breach type
            if (breachType != null) {
                predicates.add(cb.equal(root.get("breachType"), breachType));
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
