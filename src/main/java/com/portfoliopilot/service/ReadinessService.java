package com.portfoliopilot.service;

import com.portfoliopilot.dto.ReadinessDto;
import com.portfoliopilot.dto.ReadinessDto.SectionScore;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * THE distinguishing feature: weighted readiness score (0-100) plus
 * rule-based next actions. Pure backend business logic.
 *
 * Weights: Profile 15, About 10, Skills 15, Projects 20,
 *          Education 10, Experience 15, Certifications 5, Achievements 10.
 */
@Service
public class ReadinessService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;
    private final PortfolioRepository portfolioRepository;

    public ReadinessService(UserRepository userRepository,
            ProfileRepository profileRepository,
            SkillRepository skillRepository,
            ProjectRepository projectRepository,
            EducationRepository educationRepository,
            ExperienceRepository experienceRepository,
            CertificationRepository certificationRepository,
            AchievementRepository achievementRepository,
            PortfolioRepository portfolioRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public ReadinessDto readiness(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Long userId = user.getId();

        Optional<Profile> profile = profileRepository.findByUserId(userId);
        boolean hasProfile = profile.isPresent();
        boolean hasAbout = hasProfile && profile.get().getAbout() != null && !profile.get().getAbout().isBlank();
        boolean hasGithub = hasProfile && profile.get().getGithubUrl() != null && !profile.get().getGithubUrl().isBlank();
        boolean hasResume = hasProfile && profile.get().getResumeUrl() != null && !profile.get().getResumeUrl().isBlank();

        long skills = skillRepository.countByUserId(userId);
        long projects = projectRepository.countByUserId(userId);
        long education = educationRepository.countByUserId(userId);
        long experience = experienceRepository.countByUserId(userId);
        long certifications = certificationRepository.countByUserId(userId);
        long achievements = achievementRepository.countByUserId(userId);
        boolean published = portfolioRepository.findByUserId(userId)
                .map(p -> Boolean.TRUE.equals(p.getPublished())).orElse(false);

        List<SectionScore> breakdown = List.of(
                new SectionScore("profile", "Profile", 15, hasProfile ? 15 : 0, hasProfile),
                new SectionScore("about", "About", 10, hasAbout ? 10 : 0, hasAbout),
                new SectionScore("skills", "Skills", 15, skills >= 5 ? 15 : skills >= 3 ? 11 : skills >= 1 ? 7 : 0, skills >= 5),
                new SectionScore("projects", "Projects", 20, projects >= 3 ? 20 : projects == 2 ? 15 : projects == 1 ? 10 : 0, projects >= 3),
                new SectionScore("education", "Education", 10, education >= 1 ? 10 : 0, education >= 1),
                new SectionScore("experience", "Experience", 15, experience >= 1 ? 15 : 0, experience >= 1),
                new SectionScore("certifications", "Certifications", 5, certifications >= 2 ? 5 : certifications == 1 ? 3 : 0, certifications >= 2),
                new SectionScore("achievements", "Achievements", 10, achievements >= 1 ? 10 : 0, achievements >= 1));

        int score = breakdown.stream().mapToInt(SectionScore::earned).sum();
        return new ReadinessDto(score, breakdown,
                recommendations(projects, skills, education, experience, certifications, achievements,
                        hasAbout, hasGithub, hasResume, published, score));
    }

    private List<String> recommendations(long projects, long skills, long education, long experience,
            long certifications, long achievements, boolean hasAbout, boolean hasGithub,
            boolean hasResume, boolean published, int score) {
        List<String> tips = new ArrayList<>();
        if (projects == 0) {
            tips.add("Add your first project — recruiters look here first.");
        } else if (projects < 3) {
            tips.add("Add " + (3 - projects) + " more project(s) to max the Projects score.");
        }
        if (skills < 5) {
            tips.add(skills == 0 ? "Add your technical skills (aim for 5+)."
                    : "Add " + (5 - skills) + " more technical skill(s) (aim for 5+).");
        }
        if (experience == 0) {
            tips.add("Add your internship or work experience.");
        }
        if (education == 0) {
            tips.add("Add your education details.");
        }
        if (!hasAbout) {
            tips.add("Write your About story — a few lines about who you are.");
        }
        if (!hasGithub) {
            tips.add("Connect your GitHub profile.");
        }
        if (!hasResume) {
            tips.add("Link your resume so recruiters can download it.");
        }
        if (certifications == 0) {
            tips.add("Add a certification to stand out.");
        }
        if (achievements == 0) {
            tips.add("Add an achievement or award.");
        }
        if (!published && score >= 40) {
            tips.add("Publish your portfolio to get a shareable public link.");
        }
        if (tips.isEmpty()) {
            tips.add("Portfolio is launch-ready. Share your public link!");
        }
        return tips;
    }
}
