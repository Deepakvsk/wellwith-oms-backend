package com.wellwithlife.omsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String name;
    private String email;
    private String phone;
    private String refPrefix;
    private String role; // "SUPERADMIN", "ADMIN", "TEAM_LEADER", "BDE"
    private Long teamLeadId;
    private Boolean isActive;
    private String password; // Optional: Sirf jab password update karna ho
}