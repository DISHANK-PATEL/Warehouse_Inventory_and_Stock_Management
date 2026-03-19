package com.warehouse.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.inventory.dto.response.ApiResponse;
import com.warehouse.inventory.security.CustomUserDetailsService;
import com.warehouse.inventory.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter  jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper             objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors()
                .and()
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/actuator/health",
                                "/docs",
                                "/docs/**"
                        ).permitAll()

                        // Admin-only mutations
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup")        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products")            .hasAnyRole("ADMIN", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.PUT,  "/api/v1/products/**")         .hasRole("ADMIN")

                        // Bulk operations — upload is Admin only; viewing is Admin + Staff + PM
                        .requestMatchers(HttpMethod.POST, "/api/v1/bulk/upload")         .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/bulk")                .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/bulk/**")             .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")

                        // CSV exports — all roles can export their own scoped data
                        .requestMatchers(HttpMethod.GET,  "/api/v1/export/products")      .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/export/movements")     .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")

                        // Product reads
                        .requestMatchers(HttpMethod.GET,  "/api/v1/products")            .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/products/breached")   .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/products/**")         .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")

                        // Stock operations
                        .requestMatchers(HttpMethod.POST, "/api/v1/stock/update")        .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/stock/history")       .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/stock/history/**")    .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/stock/reservations")  .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")

                        // Alerts
                        .requestMatchers(HttpMethod.GET,  "/api/v1/alerts")              .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/alerts/**")           .hasAnyRole("ADMIN", "STAFF", "PRODUCT_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/alerts/*/retrigger")  .hasRole("ADMIN")

                        // Metrics (Admin only)
                        .requestMatchers(HttpMethod.GET,  "/api/v1/metrics")             .hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler(objectMapper))
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.failure("FORBIDDEN",
                            "You don't have permission to perform this action")
            ));
        };
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.failure("UNAUTHORIZED", "Authentication required. Please log in.")
            ));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}