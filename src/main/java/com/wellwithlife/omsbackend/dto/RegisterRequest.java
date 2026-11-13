package com.wellwithlife.omsbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String refPrefix;

    @NotBlank(message = "Role is required (SUPERADMIN, ADMIN, BDE)")
    private String role;

    private Long teamLeadId;
}