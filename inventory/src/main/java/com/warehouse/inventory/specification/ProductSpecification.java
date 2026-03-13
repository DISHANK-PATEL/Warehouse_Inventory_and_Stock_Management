package com.warehouse.inventory.specification;

import com.warehouse.inventory.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dynamic multi-criteria filtering for products.
 *
 * Spec requirement:
 *   "All search and filtering operations must use a dynamic, multi-criteria filtering
 *    architecture built on JPA Criteria Builder and JpaSpecification. Multiple filters
 *    can be combined in a single request. New filter fields can be added with minimal
 *    code changes."
 *
 * Each filter predicate is only added when the corresponding param is non-null,
 * so any combination of filters works correctly in a single query.
 */
public class ProductSpecification {

    private ProductSpecification() {}

    /**
     * @param search            Partial product name match (case-insensitive)
     * @param managerId         Filter by assigned Product Manager ID (Admin/Staff use)
     * @param assigned          false = only unassigned products (no PM assigned)
     * @param breachType        Filter by breach status enum value
     * @param scopedManagerId   When caller is a PM, this is their own ID — always applied
     *                          to restrict their view to only their assigned products
     */
    public static Specification<Product> withFilters(
            String search,
            UUID managerId,
            Boolean assigned,
            Product.BreachStatus breachType,
            UUID scopedManagerId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // PM scope — always applied when caller is PRODUCT_MANAGER.
            // Overrides any managerId param since a PM can only see their own products.
            if (scopedManagerId != null) {
                predicates.add(
                        cb.equal(root.get("productManager").get("id"), scopedManagerId)
                );
            }

            // Partial name search — case-insensitive LIKE
            if (search != null && !search.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%")
                );
            }

            // Filter by a specific Product Manager (Admin/Staff only — PM scope overrides this)
            if (managerId != null && scopedManagerId == null) {
                predicates.add(
                        cb.equal(root.get("productManager").get("id"), managerId)
                );
            }

            // assigned=false → only products with no Product Manager assigned
            if (Boolean.FALSE.equals(assigned)) {
                predicates.add(cb.isNull(root.get("productManager")));
            }

            // Filter by breach status
            if (breachType != null) {
                predicates.add(
                        cb.equal(root.get("breachStatus"), breachType)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Returns a Specification that matches products currently in any breach state (BELOW_MIN or ABOVE_MAX).
     * Optionally filters by a specific breach type and/or product manager.
     *
     * Used by GET /api/v1/products/breached.
     */
    public static Specification<Product> breachedProducts(
            Product.BreachStatus breachType,
            UUID managerId,
            UUID scopedManagerId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // PM scope
            if (scopedManagerId != null) {
                predicates.add(cb.equal(root.get("productManager").get("id"), scopedManagerId));
            }

            // Admin-supplied managerId filter
            if (managerId != null && scopedManagerId == null) {
                predicates.add(cb.equal(root.get("productManager").get("id"), managerId));
            }

            // Specific breach type requested
            if (breachType != null) {
                predicates.add(cb.equal(root.get("breachStatus"), breachType));
            } else {
                // No specific type → return anything that is NOT NONE (i.e. any active breach)
                predicates.add(cb.notEqual(root.get("breachStatus"), Product.BreachStatus.NONE));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}