package com.wellwithlife.omsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String refPrefix;
    private Long teamLeadId;
    private String teamLeadName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}