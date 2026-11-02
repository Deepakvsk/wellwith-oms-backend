package com.wellwithlife.omsbackend.service;

import com.wellwithlife.omsbackend.dto.RegisterRequest;
import com.wellwithlife.omsbackend.dto.UserResponse;
import com.wellwithlife.omsbackend.dto.UserUpdateRequest;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. Sabhi users fetch karna (Role filter support: TEAM_LEADER, BDE, etc.)
    public List<UserResponse> getAllUsers(String role) {
        List<User> users;
        if (role != null && !role.isBlank()) {
            users = userRepository.findByRole(role.trim().toUpperCase());
        } else {
            users = userRepository.findAll();
        }
        return users.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // 2. Single user ID se fetch karna
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return mapToResponse(user);
    }

    // 3. Admin dwara user create karna (bina token return kiye)
    @Transactional
    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String assignedRole = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole().trim().toUpperCase()
                : "BDE";

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .isActive(true)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    // 4. User update (Role, Name, Status, Optional Password)
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().trim().toUpperCase());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapToResponse(userRepository.save(user));
    }

    // 5. Active / Inactive status toggle
    @Transactional
    public UserResponse toggleUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.setIsActive(active);
        return mapToResponse(userRepository.save(user));
    }

    // 6. User delete
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        userRepository.delete(user);
    }

    // Safe Response Mapper
    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}