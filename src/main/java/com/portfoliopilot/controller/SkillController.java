package com.portfoliopilot.controller;

import com.portfoliopilot.dto.SkillDto;
import com.portfoliopilot.dto.SkillRequest;
import com.portfoliopilot.service.SkillService;
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
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public List<SkillDto> list(Authentication authentication) {
        return skillService.list(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkillDto create(Authentication authentication, @Valid @RequestBody SkillRequest request) {
        return skillService.create(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public SkillDto get(Authentication authentication, @PathVariable Long id) {
        return skillService.get(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public SkillDto update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody SkillRequest request) {
        return skillService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        skillService.delete(authentication.getName(), id);
    }
}
