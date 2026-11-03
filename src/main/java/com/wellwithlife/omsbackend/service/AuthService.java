package com.wellwithlife.omsbackend.service;

import com.wellwithlife.omsbackend.config.JwtService;
import com.wellwithlife.omsbackend.dto.AuthRequest;
import com.wellwithlife.omsbackend.dto.AuthResponse;
import com.wellwithlife.omsbackend.dto.RegisterRequest;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String assignedRole = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().toUpperCase()
                : "BDE";

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
//                .phone(request.getPhone()) // Phone mapping included
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        var userDetails = new org.springframework.security.core.userdetails.User(
                savedUser.getEmail(),
                savedUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole()))
        );

        String token = jwtService.generateToken(userDetails, savedUser.getUserId(), savedUser.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .refPrefix(user.getRefPrefix())
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        // 1. User existence & active check
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Safe boolean check (null ko safe default true maanein)
        boolean isEnabled = user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive());

        if (!isEnabled) {
            throw new DisabledException("Your account is deactivated. Please contact administrator.");
        }

        // 2. Credentials verification
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 3. UserDetails mapping with primitive boolean
        var userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                isEnabled,
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );

        String token = jwtService.generateToken(userDetails, user.getUserId(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .refPrefix(user.getRefPrefix())
                .build();
    }
}