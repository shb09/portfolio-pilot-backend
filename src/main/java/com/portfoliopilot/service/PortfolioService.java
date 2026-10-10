package com.portfoliopilot.service;

import com.portfoliopilot.dto.AchievementDto;
import com.portfoliopilot.dto.CertificationDto;
import com.portfoliopilot.dto.EducationDto;
import com.portfoliopilot.dto.ExperienceDto;
import com.portfoliopilot.dto.PortfolioDto;
import com.portfoliopilot.dto.PortfolioRequest;
import com.portfoliopilot.dto.ProjectDto;
import com.portfoliopilot.dto.PublicPortfolioDto;
import com.portfoliopilot.dto.SkillDto;
import com.portfoliopilot.entity.Portfolio;
import com.portfoliopilot.entity.Profile;
import com.portfoliopilot.entity.User;
import com.portfoliopilot.repository.AchievementRepository;
import com.portfoliopilot.repository.CertificationRepository;
import com.portfoliopilot.repository.EducationRepository;
import com.portfoliopilot.repository.ExperienceRepository;
import com.portfoliopilot.repository.PortfolioRepository;
import com.portfoliopilot.repository.ProfileRepository;
import com.portfoliopilot.repository.ProjectRepository;
import com.portfoliopilot.repository.SkillRepository;
import com.portfoliopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Edit -> Preview (same assembly as public, auth required) -> Publish.
 * Public assembly is refused (404) unless published=true.
 */
@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;

    public PortfolioService(PortfolioRepository portfolioRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            SkillRepository skillRepository,
            ProjectRepository projectRepository,
            ExperienceRepository experienceRepository,
            EducationRepository educationRepository,
            CertificationRepository certificationRepository,
            AchievementRepository achievementRepository) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
    }

    private User owner(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public PortfolioDto getMine(String email) {
        User user = owner(email);
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not set up yet"));
        return PortfolioDto.from(portfolio);
    }

    @Transactional
    public PortfolioDto upsert(String email, PortfolioRequest request) {
        User user = owner(email);
        String username = request.username().toLowerCase();
        portfolioRepository.findByUsername(username).ifPresent(existing -> {
            if (!existing.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
            }
        });
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId()).orElseGet(() -> {
            Portfolio p = new Portfolio();
            p.setUser(user);
            return p;
        });
        portfolio.setUsername(username);
        portfolio.setPublished(request.published());
        portfolio.setTagline(request.tagline());
        return PortfolioDto.from(portfolioRepository.save(portfolio));
    }

    /** Preview = public assembly of MY data, even before publishing. */
    public PublicPortfolioDto preview(String email) {
        User user = owner(email);
        Portfolio portfolio = portfolioRepository.findByUserId(user.getId()).orElse(null);
        return assemble(user, portfolio);
    }

    /** Public page: 404 unless the slug exists AND is published. */
    public PublicPortfolioDto getPublic(String username) {
        Portfolio portfolio = portfolioRepository.findByUsername(username.toLowerCase())
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
        return assemble(portfolio.getUser(), portfolio);
    }

    private PublicPortfolioDto assemble(User user, Portfolio portfolio) {
        Long userId = user.getId();
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        List<SkillDto> skills = skillRepository.findByUserIdOrderByIdDesc(userId).stream().map(SkillDto::from).toList();
        List<ProjectDto> projects = projectRepository.findByUserIdOrderByIdDesc(userId).stream().map(ProjectDto::from).toList();
        List<ExperienceDto> experience = experienceRepository.findByUserIdOrderByIdDesc(userId).stream().map(ExperienceDto::from).toList();
        List<EducationDto> education = educationRepository.findByUserIdOrderByIdDesc(userId).stream().map(EducationDto::from).toList();
        List<CertificationDto> certifications = certificationRepository.findByUserIdOrderByIdDesc(userId).stream().map(CertificationDto::from).toList();
        List<AchievementDto> achievements = achievementRepository.findByUserIdOrderByIdDesc(userId).stream().map(AchievementDto::from).toList();
        return new PublicPortfolioDto(
                portfolio == null ? null : portfolio.getUsername(),
                portfolio == null ? null : portfolio.getTagline(),
                user.getName(),
                profile == null ? null : profile.getHeadline(),
                profile == null ? null : profile.getAbout(),
                profile == null ? null : profile.getLocation(),
                profile == null ? null : profile.getGithubUrl(),
                profile == null ? null : profile.getLinkedinUrl(),
                profile == null ? null : profile.getResumeUrl(),
                skills, projects, experience, education, certifications, achievements);
    }
}
