package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.request.LoginRequest;
import com.warehouse.inventory.dto.request.SignupRequest;
import com.warehouse.inventory.dto.response.AuthResponse;
import com.warehouse.inventory.dto.response.SignupResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    SignupResponse signup(SignupRequest request);
}