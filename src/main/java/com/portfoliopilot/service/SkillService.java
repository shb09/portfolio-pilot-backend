package com.portfoliopilot.service;

import com.portfoliopilot.dto.SkillDto;
import com.portfoliopilot.dto.SkillRequest;
import com.portfoliopilot.entity.Skill;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.SkillRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Same ownership template as ProjectService. */
@Service
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    public SkillService(SkillRepository skillRepository, UserRepository userRepository) {
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Skill owned(Long id, Long userId) {
        return skillRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
    }

    public List<SkillDto> list(String email) {
        return skillRepository.findByUserIdOrderByIdDesc(owner(email).getId()).stream()
                .map(SkillDto::from).toList();
    }

    @Transactional
    public SkillDto create(String email, SkillRequest request) {
        Skill skill = new Skill();
        skill.setUser(owner(email));
        skill.setName(request.name());
        skill.setLevel(request.level());
        return SkillDto.from(skillRepository.save(skill));
    }

    public SkillDto get(String email, Long id) {
        return SkillDto.from(owned(id, owner(email).getId()));
    }

    @Transactional
    public SkillDto update(String email, Long id, SkillRequest request) {
        Skill skill = owned(id, owner(email).getId());
        skill.setName(request.name());
        skill.setLevel(request.level());
        return SkillDto.from(skillRepository.save(skill));
    }

    @Transactional
    public void delete(String email, Long id) {
        skillRepository.delete(owned(id, owner(email).getId()));
    }
}
