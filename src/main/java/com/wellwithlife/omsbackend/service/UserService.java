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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Role-scoped user listing
    @Transactional(readOnly = true)
    public List<UserResponse> getScopedUsers(String roleFilter, User currentUser) {
        String callerRole = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "BDE";
        boolean isAdmin = "SUPERADMIN".equals(callerRole) || "ADMIN".equals(callerRole);

        List<User> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(u -> {
                    if (isAdmin) return true;
                    if ("TEAM_LEADER".equals(callerRole)) {
                        // Team Lead sees themselves + their assigned BDE members only
                        boolean isSelf = Objects.equals(u.getUserId(), currentUser.getUserId());
                        boolean isMyMember = u.getTeamLead() != null &&
                                Objects.equals(u.getTeamLead().getUserId(), currentUser.getUserId());
                        return isSelf || isMyMember;
                    }
                    return Objects.equals(u.getUserId(), currentUser.getUserId());
                })
                .filter(u -> roleFilter == null || roleFilter.isBlank() ||
                        roleFilter.trim().equalsIgnoreCase(u.getRole()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return mapToResponse(user);
    }

    // Create User with Strict Team Lead Auto-Assignment
    @Transactional
    public UserResponse createUser(RegisterRequest request, User currentUser) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String callerRole = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "BDE";
        boolean isTeamLead = "TEAM_LEADER".equals(callerRole);

        String assignedRole;
        User assignedTeamLead = null;

        if (isTeamLead) {
            // Security Guard: Team Lead can ONLY create BDEs under their own team
            assignedRole = "BDE";
            assignedTeamLead = currentUser;
        } else {
            assignedRole = (request.getRole() != null && !request.getRole().isBlank())
                    ? request.getRole().trim().toUpperCase()
                    : "BDE";

            if ("BDE".equals(assignedRole) && request.getTeamLeadId() != null) {
                User tl = userRepository.findById(request.getTeamLeadId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected Team Leader not found."));
                if (!"TEAM_LEADER".equalsIgnoreCase(tl.getRole())) {
                    throw new IllegalArgumentException("Assigned manager must have TEAM_LEADER role.");
                }
                assignedTeamLead = tl;
            }
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .teamLead(assignedTeamLead)
                .isActive(true)
                .refPrefix(request.getRefPrefix() != null && !request.getRefPrefix().isBlank()
                        ? request.getRefPrefix().trim().toUpperCase()
                        : null)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    // Update User with Team Lead Boundary Enforcement
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String callerRole = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "BDE";
        boolean isTeamLead = "TEAM_LEADER".equals(callerRole);

        if (isTeamLead) {
            boolean isMyMember = user.getTeamLead() != null &&
                    Objects.equals(user.getTeamLead().getUserId(), currentUser.getUserId());
            if (!isMyMember) {
                throw new SecurityException("Access denied: You can only edit members of your own team.");
            }
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        if (!isTeamLead && request.getRole() != null && !request.getRole().isBlank()) {
            String nextRole = request.getRole().trim().toUpperCase();
            user.setRole(nextRole);
            if (!"BDE".equals(nextRole)) {
                user.setTeamLead(null);
            }
        }

        if (!isTeamLead && "BDE".equalsIgnoreCase(user.getRole())) {
            if (request.getTeamLeadId() != null) {
                if (request.getTeamLeadId() == 0L) {
                    user.setTeamLead(null);
                } else {
                    User tl = userRepository.findById(request.getTeamLeadId())
                            .orElseThrow(() -> new IllegalArgumentException("Selected Team Leader not found."));
                    user.setTeamLead(tl);
                }
            }
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRefPrefix() != null) {
            String prefix = request.getRefPrefix().trim().toUpperCase();
            user.setRefPrefix(prefix.isEmpty() ? null : prefix);
        }

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId, boolean active, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        enforceTeamScope(user, currentUser);
        user.setIsActive(active);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, User currentUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        enforceTeamScope(user, currentUser);
        userRepository.delete(user);
    }

    private void enforceTeamScope(User targetUser, User currentUser) {
        String callerRole = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "BDE";
        if ("TEAM_LEADER".equals(callerRole)) {
            boolean isMyMember = targetUser.getTeamLead() != null &&
                    Objects.equals(targetUser.getTeamLead().getUserId(), currentUser.getUserId());
            if (!isMyMember) {
                throw new SecurityException("Access denied: User does not belong to your team.");
            }
        }
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .refPrefix(user.getRefPrefix())
                .teamLeadId(user.getTeamLead() != null ? user.getTeamLead().getUserId() : null)
                .teamLeadName(user.getTeamLead() != null ? user.getTeamLead().getName() : null)
                .isActive(user.getIsActive() == null || Boolean.TRUE.equals(user.getIsActive()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}