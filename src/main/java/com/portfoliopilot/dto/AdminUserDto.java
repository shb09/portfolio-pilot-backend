package com.portfoliopilot.dto;

import com.portfoliopilot.entity.User;

/** Admin user row. No hashes, no tokens — ever. */
public record AdminUserDto(Long id, String username, String name, String email,
        String role, boolean emailVerified, boolean enabled, String createdAt) {

    public static AdminUserDto from(User u) {
        return new AdminUserDto(u.getId(), u.getUsername(), u.getName(), u.getEmail(),
                u.getRole() == null ? null : u.getRole().name(),
                Boolean.TRUE.equals(u.getEmailVerified()), Boolean.TRUE.equals(u.getEnabled()),
                u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
    }
}
