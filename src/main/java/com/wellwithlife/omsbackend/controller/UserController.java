package com.wellwithlife.omsbackend.controller;

import com.wellwithlife.omsbackend.dto.UserDto;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Sabhi BDE / BDM users ki list return karta hai
     * Route: GET /api/users/bdes
     */
    @GetMapping("/bdes")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<List<UserDto>> getBdeUsers() {
        List<User> bdes = userRepository.findByRole("BDE");

        List<UserDto> response = bdes.stream()
                .map(u -> UserDto.builder()
                        .userId(u.getUserId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * System ke saare users return karta hai (User Management Page ke liye)
     * Route: GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> response = userRepository.findAll().stream()
                .map(u -> UserDto.builder()
                        .userId(u.getUserId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}