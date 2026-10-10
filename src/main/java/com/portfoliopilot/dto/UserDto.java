package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Role;
import com.portfoliopilot.entity.User;

/** Safe user shape. Role drives frontend routing; never password material. */
public record UserDto(Long id, String username, String name, String email, Role role, boolean emailVerified) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getEmail(),
                user.getRole(), Boolean.TRUE.equals(user.getEmailVerified()));
    }
}
