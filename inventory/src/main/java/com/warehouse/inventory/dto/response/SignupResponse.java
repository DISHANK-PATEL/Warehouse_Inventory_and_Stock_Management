package com.warehouse.inventory.dto.response;

import com.warehouse.inventory.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SignupResponse {

    private final Integer id;
    private final String fullName;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;

    public SignupResponse(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.createdAt = user.getCreatedAt();
    }
}