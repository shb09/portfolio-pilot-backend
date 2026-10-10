package com.portfoliopilot.service;

import com.portfoliopilot.dto.AnalyticsEventRequest;
import com.portfoliopilot.dto.AnalyticsSummaryDto;
import com.portfoliopilot.entity.AnalyticsEvent;
import com.portfoliopilot.entity.EventType;
import com.portfoliopilot.entity.Portfolio;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.AnalyticsEventRepository;
import com.portfoliopilot.repository.PortfolioRepository;
import com.portfoliopilot.repository.ProjectRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public ingest (fire-and-forget from the portfolio page) +
 * private summary for the owner's dashboard.
 */
@Service
public class AnalyticsService {

    private final AnalyticsEventRepository eventRepository;
    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public AnalyticsService(AnalyticsEventRepository eventRepository,
            PortfolioRepository portfolioRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.portfolioRepository = portfolioRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(AnalyticsEventRequest request) {
        Portfolio portfolio = portfolioRepository.findByUsername(request.username().toLowerCase())
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        Long projectId = request.projectId();
        if (projectId != null && projectRepository.findByIdAndUserId(projectId, portfolio.getUser().getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown project for this portfolio");
        }
        AnalyticsEvent event = new AnalyticsEvent();
        event.setOwnerUser(portfolio.getUser());
        event.setEventType(request.eventType());
        event.setProjectId(projectId);
        eventRepository.save(event);
    }

    public AnalyticsSummaryDto summary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Long userId = user.getId();
        long views = count(userId, EventType.PORTFOLIO_VIEW);
        long project = count(userId, EventType.PROJECT_CLICK);
        long github = count(userId, EventType.GITHUB_CLICK);
        long resume = count(userId, EventType.RESUME_CLICK);
        long linkedin = count(userId, EventType.LINKEDIN_CLICK);
        return new AnalyticsSummaryDto(views, project, github, resume, linkedin,
                views + project + github + resume + linkedin);
    }

    private long count(Long userId, EventType type) {
        return eventRepository.countByOwnerUserIdAndEventType(userId, type);
    }
}
