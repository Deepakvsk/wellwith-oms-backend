package com.wellwithlife.omsbackend.controller;

import com.wellwithlife.omsbackend.dto.RegisterRequest;
import com.wellwithlife.omsbackend.dto.UserResponse;
import com.wellwithlife.omsbackend.dto.UserUpdateRequest;
import com.wellwithlife.omsbackend.entity.User;
import com.wellwithlife.omsbackend.repository.UserRepository;
import com.wellwithlife.omsbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found: " + email));
    }

    @GetMapping({"/bde-list", "/bdes"})
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<List<UserResponse>> getBdes(Authentication auth) {
        return ResponseEntity.ok(userService.getScopedUsers("BDE", getAuthenticatedUser(auth)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            Authentication auth
    ) {
        return ResponseEntity.ok(userService.getScopedUsers(role, getAuthenticatedUser(auth)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody RegisterRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(userService.createUser(request, getAuthenticatedUser(auth)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request,
            Authentication auth
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request, getAuthenticatedUser(auth)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<UserResponse> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            Authentication auth
    ) {
        return ResponseEntity.ok(userService.toggleUserStatus(id, active, getAuthenticatedUser(auth)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication auth) {
        userService.deleteUser(id, getAuthenticatedUser(auth));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deleted successfully",
                "userId", id
        ));
    }
}