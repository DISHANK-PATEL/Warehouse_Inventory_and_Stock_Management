package com.warehouse.inventory.controller;

import com.warehouse.inventory.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Optional — only present if spring-boot-maven-plugin build-info goal is configured.
     * Injected as Optional so the app still starts without it.
     */
    private final Optional<BuildProperties> buildProperties;

    /**
     * GET /api/v1/health
     *
     * Lightweight custom health check — no auth required (permitted in SecurityConfig).
     * Returns: status, version, uptime, db connectivity, timestamp.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {

        Map<String, Object> health = new LinkedHashMap<>();

        health.put("status",    "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("uptime",    formatUptime());
        health.put("version",   resolveVersion());
        health.put("database",  checkDatabase());

        return ResponseEntity.ok(ApiResponse.success(health));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Checks DB connectivity with a simple SELECT 1.
     * Returns a map with status UP/DOWN and latency in ms.
     */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            db.put("status",    "UP");
            db.put("latencyMs", System.currentTimeMillis() - start);
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error",  e.getMessage());
        }
        return db;
    }

    /**
     * JVM uptime formatted as Xd Xh Xm Xs.
     */
    private String formatUptime() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long days    = TimeUnit.MILLISECONDS.toDays(uptimeMs);
        long hours   = TimeUnit.MILLISECONDS.toHours(uptimeMs)   % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMs) % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    /**
     * Reads version from build-info if available, falls back to manifest,
     * then to a default string.
     */
    private String resolveVersion() {
        return buildProperties
                .map(BuildProperties::getVersion)
                .orElseGet(() -> {
                    String manifestVersion = getClass().getPackage().getImplementationVersion();
                    return manifestVersion != null ? manifestVersion : "1.0.0-dev";
                });
    }
}