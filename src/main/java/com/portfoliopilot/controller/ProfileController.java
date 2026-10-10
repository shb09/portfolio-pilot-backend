package com.portfoliopilot.controller;

import com.portfoliopilot.dto.ProfileDto;
import com.portfoliopilot.dto.ProfileRequest;
import com.portfoliopilot.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileDto get(Authentication authentication) {
        return profileService.get(authentication.getName());
    }

    /** Creates the profile on first call, updates after that. */
    @PutMapping
    public ProfileDto upsert(Authentication authentication, @Valid @RequestBody ProfileRequest request) {
        return profileService.upsert(authentication.getName(), request);
    }
}
