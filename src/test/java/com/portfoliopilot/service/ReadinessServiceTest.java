package com.portfoliopilot.service;

import com.portfoliopilot.dto.ReadinessDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Proves the readiness math without a database: repositories are mocked. */
@ExtendWith(MockitoExtension.class)
class ReadinessServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private EducationRepository educationRepository;
    @Mock
    private ExperienceRepository experienceRepository;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private ReadinessService service;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        Field id = User.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(user, 1L);
        user.setEmail("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
    }

    private void counts(long skills, long projects, long education, long experience, long certs, long ach) {
        when(skillRepository.countByUserId(1L)).thenReturn(skills);
        when(projectRepository.countByUserId(1L)).thenReturn(projects);
        when(educationRepository.countByUserId(1L)).thenReturn(education);
        when(experienceRepository.countByUserId(1L)).thenReturn(experience);
        when(certificationRepository.countByUserId(1L)).thenReturn(certs);
        when(achievementRepository.countByUserId(1L)).thenReturn(ach);
    }

    @Test
    void emptyProfileScoresZeroWithActionableTips() {
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.empty());
        counts(0, 0, 0, 0, 0, 0);

        ReadinessDto dto = service.readiness("student@test.com");

        assertEquals(0, dto.score());
        assertEquals(8, dto.breakdown().size());
        assertTrue(dto.recommendations().stream().anyMatch(t -> t.contains("first project")));
        assertTrue(dto.recommendations().stream().anyMatch(t -> t.contains("5+")));
    }

    @Test
    void completePublishedPortfolioScoresHundred() {
        Profile profile = new Profile();
        profile.setAbout("About me");
        profile.setGithubUrl("https://github.com/me");
        profile.setResumeUrl("https://example.com/resume.pdf");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        Portfolio portfolio = new Portfolio();
        portfolio.setPublished(true);
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.of(portfolio));
        counts(5, 3, 1, 1, 2, 1);

        ReadinessDto dto = service.readiness("student@test.com");

        assertEquals(100, dto.score());
        assertTrue(dto.breakdown().stream().allMatch(ReadinessDto.SectionScore::done));
        assertEquals(1, dto.recommendations().size());
        assertTrue(dto.recommendations().get(0).contains("launch-ready"));
    }

    @Test
    void partialCountsEarnPartialWeights() {
        Profile profile = new Profile();
        profile.setAbout("About me");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(portfolioRepository.findByUserId(1L)).thenReturn(Optional.empty());
        counts(2, 1, 1, 0, 1, 0);

        ReadinessDto dto = service.readiness("student@test.com");

        // 15 profile + 10 about + 7 skills + 10 projects + 10 education + 0 exp + 3 certs + 0 ach = 55
        assertEquals(55, dto.score());
    }
}
