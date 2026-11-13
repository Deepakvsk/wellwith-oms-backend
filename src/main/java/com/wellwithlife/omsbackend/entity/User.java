package com.wellwithlife.omsbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

//    @Column(nullable = false, length = 20)
//    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(name = "ref_prefix", length = 50)
    private String refPrefix;

    @Column(nullable = false, length = 30)
    private String role; // SUPERADMIN, ADMIN, BDE

    // Self-Referencing Team Lead Hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_lead_id")
    private User teamLead;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    // ==========================================
    // Spring Security UserDetails Implementation
    // ==========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String cleanRole = (this.role != null && !this.role.isBlank())
                ? this.role.trim().toUpperCase()
                : "BDE";

        String plainRole = cleanRole.startsWith("ROLE_") ? cleanRole.substring(5) : cleanRole;
        String prefixedRole = "ROLE_" + plainRole;

        // Dono variations return honge: "SUPERADMIN" aur "ROLE_SUPERADMIN"
        return List.of(
                new SimpleGrantedAuthority(plainRole),
                new SimpleGrantedAuthority(prefixedRole)
        );
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.isActive);
    }
}