package com.warehouse.inventory.service;

import com.warehouse.inventory.dto.request.LoginRequest;
import com.warehouse.inventory.dto.request.SignupRequest;
import com.warehouse.inventory.dto.response.AuthResponse;
import com.warehouse.inventory.dto.response.SignupResponse;
import com.warehouse.inventory.entity.User;
import com.warehouse.inventory.exception.ConflictException;
import com.warehouse.inventory.repository.UserRepository;
import com.warehouse.inventory.security.CustomUserDetails;
import com.warehouse.inventory.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiration() / 1000,
                userDetails.getUsername(),
                userDetails.getUser().getRole().name()
        );
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    "An account with email '" + request.getEmail() + "' already exists");
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.STAFF)
                .isActive(true)
                .build();

        User saved = userRepository.save(newUser);
        return new SignupResponse(saved);
    }
}