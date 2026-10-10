package com.portfoliopilot.service;

import com.portfoliopilot.dto.ProfileDto;
import com.portfoliopilot.dto.ProfileRequest;
import com.portfoliopilot.entity.Profile;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.ProfileRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Get-or-404 + upsert. One profile per user, resolved by JWT email. */
@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public ProfileDto get(String email) {
        User user = owner(email);
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return ProfileDto.from(profile);
    }

    @Transactional
    public ProfileDto upsert(String email, ProfileRequest request) {
        User user = owner(email);
        Profile profile = profileRepository.findByUserId(user.getId()).orElseGet(() -> {
            Profile p = new Profile();
            p.setUser(user);
            return p;
        });
        profile.setHeadline(request.headline());
        profile.setAbout(request.about());
        profile.setLocation(request.location());
        profile.setGithubUrl(request.githubUrl());
        profile.setLinkedinUrl(request.linkedinUrl());
        profile.setResumeUrl(request.resumeUrl());
        return ProfileDto.from(profileRepository.save(profile));
    }
}
