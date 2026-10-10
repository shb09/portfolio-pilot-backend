package com.portfoliopilot.controller;

import com.portfoliopilot.dto.AchievementDto;
import com.portfoliopilot.dto.AchievementRequest;
import com.portfoliopilot.service.AchievementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public List<AchievementDto> list(Authentication authentication) {
        return achievementService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AchievementDto create(Authentication authentication, @Valid @RequestBody AchievementRequest request) {
        return achievementService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public AchievementDto get(Authentication authentication, @PathVariable Long id) {
        return achievementService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public AchievementDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody AchievementRequest request) {
        return achievementService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        achievementService.delete(authentication.getName(), id);
    }
}
