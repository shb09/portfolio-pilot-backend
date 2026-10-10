package com.portfoliopilot.service;

import com.portfoliopilot.dto.AdminStatsDto;
import com.portfoliopilot.dto.AdminUserDto;
import com.portfoliopilot.entity.Role;
import com.portfoliopilot.repository.AnalyticsEventRepository;
import com.portfoliopilot.repository.PortfolioRepository;
import com.portfoliopilot.repository.ProjectRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Admin-only reads. Every number traces to a repository query — nothing fabricated. */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;
    private final AnalyticsEventRepository eventRepository;

    public AdminService(UserRepository userRepository,
            PortfolioRepository portfolioRepository,
            ProjectRepository projectRepository,
            AnalyticsEventRepository eventRepository) {
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
    }

    public AdminStatsDto stats() {
        long total = userRepository.count();
        long verified = userRepository.countByEmailVerified(true);
        return new AdminStatsDto(
                total,
                verified,
                total - verified,
                portfolioRepository.count(),
                portfolioRepository.countByPublished(true),
                projectRepository.count(),
                eventRepository.count(),
                userRepository.findTop5ByOrderByCreatedAtDesc().stream().map(AdminUserDto::from).toList());
    }

    public List<AdminUserDto> users() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(AdminUserDto::from)
                .toList();
    }
}
