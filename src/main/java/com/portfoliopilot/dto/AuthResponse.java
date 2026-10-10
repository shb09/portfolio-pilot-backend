package com.portfoliopilot.dto;

import com.portfoliopilot.entity.Role;

/** Returned by login only — never by registration (see RegisterResponse). */
public record AuthResponse(String token, UserDto user, Role role) {

    public AuthResponse(String token, UserDto user) {
        this(token, user, user.role());
    }
}
