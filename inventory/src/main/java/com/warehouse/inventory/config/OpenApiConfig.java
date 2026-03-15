package com.warehouse.inventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI warehouseOpenAPI() {

        return new OpenAPI()

                // ── Info ─────────────────────────────────────────────────
                .info(new Info()
                        .title("Warehouse Inventory & Stock Management API")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Warehouse Dev Team")
                                .email("admin@warehouse.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                )

                // ── Servers ───────────────────────────────────────────────
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development"),
                        new Server()
                                .url("https://api.warehouse.example.com")
                                .description("Production (example)")
                ))

                // ── Security ──────────────────────────────────────────────
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. Obtain it from POST /api/v1/auth/login.")
                        )
                )

                // ── Tags (control display order in Swagger UI) ────────────
                .tags(List.of(
                        new Tag().name("Authentication")
                                .description("Login and user registration"),

                        new Tag().name("Products")
                                .description("Create, update, list, and monitor products. "
                                        + "Product Managers see only their assigned products."),

                        new Tag().name("Stock")
                                .description("ADD, REMOVE, RESERVE, RELEASE stock. "
                                        + "Every operation is recorded in the movement history."),

                        new Tag().name("Stock Reservations")
                                .description("View active reservations. "
                                        + "Expired reservations are auto-released by the scheduler."),

                        new Tag().name("Stock Alerts")
                                .description("Threshold breach alerts (BELOW_MIN / ABOVE_MAX). "
                                        + "Admins can retrigger failed email notifications."),

                        new Tag().name("Metrics")
                                .description("Admin-only business metrics over a configurable time window. "
                                        + "Use ?hours=N or ?from=&to= query params."),

                        new Tag().name("Bulk Operations")
                                .description("Admin CSV bulk product upload with async job tracking."),

                        new Tag().name("Export")
                                .description("Download products and stock movements as CSV files."),

                        new Tag().name("Health")
                                .description("Application and database health check.")
                ));
    }
}