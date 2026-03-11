package com.warehouse.inventory.config;

import com.warehouse.inventory.entity.StockReservation;
import com.warehouse.inventory.repository.ProductRepository;
import com.warehouse.inventory.repository.StockReservationRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry              meterRegistry;
    private final ProductRepository          productRepository;
    private final StockReservationRepository reservationRepository;

    @PostConstruct
    public void registerGauges() {
        Gauge.builder("products.total",
                productRepository, repo -> (double) repo.count())
                .description("Total number of products in the warehouse")
                .register(meterRegistry);

        Gauge.builder("stock.reservations.active",
                        reservationRepository,
                        repo -> (double) repo.countByStatus(StockReservation.Status.ACTIVE))
                .description("Number of currently active stock reservations")
                .register(meterRegistry);
    }
}
