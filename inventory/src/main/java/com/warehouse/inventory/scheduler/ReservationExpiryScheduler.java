package com.warehouse.inventory.scheduler;

import com.warehouse.inventory.entity.Product;
import com.warehouse.inventory.entity.StockReservation;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that automatically expires ACTIVE stock reservations
 * whose expiresAt time has passed.
 *
 * For each expired reservation:
 *   1. Marks the reservation status → EXPIRED
 *   2. Decrements product.reservedQuantity by the reservation's quantity
 *      so the stock becomes available again
 *
 * Runs every 5 minutes by default (configurable via scheduler.expiry-interval-ms).
 */
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final StockReservationRepository reservationRepository;
    private final ProductRepository          productRepository;

    @Scheduled(
            fixedDelayString  = "${scheduler.expiry-interval-ms:300000}",
            initialDelayString = "${scheduler.expiry-initial-delay-ms:30000}"
    )
    @Transactional
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<StockReservation> expired =
                reservationRepository.findByStatusAndExpiresAtBefore(
                        StockReservation.Status.ACTIVE, now);

        if (expired.isEmpty()) {
            logger.debug("ReservationExpiryScheduler: no expired reservations found");
            return;
        }

        logger.info("ReservationExpiryScheduler: expiring {} reservation(s)", expired.size());

        for (StockReservation reservation : expired) {
            Product product = reservation.getProduct();

            // Return reserved quantity back to available pool
            int restored = product.getReservedQuantity() - reservation.getQuantity();
            product.setReservedQuantity(Math.max(0, restored)); // guard against negative
            productRepository.save(product);

            // Mark reservation as expired
            reservation.setStatus(StockReservation.Status.EXPIRED);
            reservationRepository.save(reservation);

            logger.info("Reservation {} expired for product '{}' — {} units restored",
                    reservation.getId(), product.getName(), reservation.getQuantity());
        }
    }
}