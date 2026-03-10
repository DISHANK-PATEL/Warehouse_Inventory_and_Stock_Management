package com.warehouse.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;

    private UserInfo user;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String email;
        private String role;
    }
}