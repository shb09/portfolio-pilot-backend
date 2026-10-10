package com.portfoliopilot.service;

import com.portfoliopilot.dto.AchievementDto;
import com.portfoliopilot.dto.AchievementRequest;
import com.portfoliopilot.entity.Achievement;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.AchievementRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;

    public AchievementService(AchievementRepository achievementRepository, UserRepository userRepository) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Achievement owned(Long id, Long userId) {
        return achievementRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
    }

    public List<AchievementDto> list(String email) {
        return achievementRepository.findByUserIdOrderByIdDesc(owner(email).getId()).stream()
                .map(AchievementDto::from).toList();
    }

    @Transactional
    public AchievementDto create(String email, AchievementRequest request) {
        Achievement achievement = new Achievement();
        achievement.setUser(owner(email));
        apply(achievement, request);
        return AchievementDto.from(achievementRepository.save(achievement));
    }

    public AchievementDto get(String email, Long id) {
        return AchievementDto.from(owned(id, owner(email).getId()));
    }

    @Transactional
    public AchievementDto update(String email, Long id, AchievementRequest request) {
        Achievement achievement = owned(id, owner(email).getId());
        apply(achievement, request);
        return AchievementDto.from(achievementRepository.save(achievement));
    }

    @Transactional
    public void delete(String email, Long id) {
        achievementRepository.delete(owned(id, owner(email).getId()));
    }

    private void apply(Achievement achievement, AchievementRequest request) {
        achievement.setTitle(request.title());
        achievement.setDescription(request.description());
        achievement.setDate(request.date());
        achievement.setLink(request.link());
    }
}
