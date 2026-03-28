package service;

import model.SkillMatchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchingServiceTest {
    private final MatchingService matchingService = new MatchingService();

    @Test
    void shouldCalculateExpectedScoreAndMissingSkills() {
        SkillMatchResult result = matchingService.calculateMatch(
                List.of("Java", "Communication"),
                List.of("Java", "Agile", "Communication")
        );

        assertEquals(67, result.getScorePercentage());
        assertEquals(List.of("Java", "Communication"), result.getMatchedSkills());
        assertEquals(List.of("Agile"), result.getMissingSkills());
        assertTrue(result.getExplanation().contains("67%"));
    }

    @Test
    void shouldAllowConfiguredSynonymMatch() {
        SkillMatchResult result = matchingService.calculateMatch(
                List.of("Scrum", "Presentation"),
                List.of("Agile", "Communication")
        );

        assertEquals(100, result.getScorePercentage());
    }
}
