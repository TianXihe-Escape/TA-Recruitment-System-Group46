package service;

import model.SkillMatchResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based skill matching service.
 * The algorithm is intentionally simple and explainable so match outcomes can
 * be shown in demos and reviewed by staff without hidden machine-learning logic.
 */
public class MatchingService {
    /**
     * Small synonym map used to recognize closely related skills without making
     * the matching behaviour opaque.
     */
    // Keep the synonym list small and explainable so the matching result is easy to justify in demos.
    private static final Map<String, Set<String>> SYNONYMS = Map.of(
            "communication", Set.of("presentation", "teamwork"),
            "data analysis", Set.of("analytics", "data analytics"),
            "agile", Set.of("scrum"),
            "tutoring", Set.of("mentoring", "teaching")
    );

    /**
     * Calculates a match result between applicant skills and required job skills.
     *
     * @param applicantSkills skills declared by the applicant
     * @param requiredSkills skills required by the job posting
     * @return explainable match result including score, matched skills, and missing skills
     */
    public SkillMatchResult calculateMatch(List<String> applicantSkills, List<String> requiredSkills) {
        SkillMatchResult result = new SkillMatchResult();
        List<String> normalizedApplicant = normalizeSkills(applicantSkills);
        List<String> normalizedRequired = normalizeSkills(requiredSkills);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String required : normalizedRequired) {
            if (normalizedApplicant.contains(required) || hasSynonymMatch(required, normalizedApplicant)) {
                matched.add(required);
            } else {
                missing.add(required);
            }
        }

        int score = normalizedRequired.isEmpty() ? 0 :
                (int) Math.round((matched.size() * 100.0) / normalizedRequired.size());

        result.setScorePercentage(score);
        result.setMatchedSkills(restoreDisplayOrder(requiredSkills, matched));
        result.setMissingSkills(restoreDisplayOrder(requiredSkills, missing));
        result.setExplanation(buildExplanation(requiredSkills, result.getMatchedSkills(), result.getMissingSkills(), score));
        return result;
    }

    /**
     * Checks whether any configured synonym for a required skill appears in the
     * applicant skill list.
     */
    private boolean hasSynonymMatch(String required, List<String> applicantSkills) {
        return SYNONYMS.getOrDefault(required, Set.of()).stream().anyMatch(applicantSkills::contains);
    }

    /**
     * Converts a skill list to a normalized, lower-case, distinct representation
     * for comparison.
     */
    private List<String> normalizeSkills(List<String> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Rebuilds a subset of required skills using the original job-entered order so
     * UI output remains familiar to module organizers.
     */
    private List<String> restoreDisplayOrder(List<String> originalRequiredSkills, List<String> normalizedSubset) {
        if (originalRequiredSkills == null) {
            return List.of();
        }
        // Return skills in the job's original order so UI output matches what the MO entered.
        return originalRequiredSkills.stream()
                .filter(skill -> normalizedSubset.contains(skill.trim().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }

    /**
     * Builds a short human-readable explanation used in dialogs and tables.
     */
    private String buildExplanation(List<String> requiredSkills, List<String> matchedSkills, List<String> missingSkills, int score) {
        return "Match score " + score + "% based on " + matchedSkills.size() + " matched skill(s) out of "
                + (requiredSkills == null ? 0 : requiredSkills.size()) + ". Missing: "
                + (missingSkills.isEmpty() ? "none" : String.join(", ", missingSkills)) + ".";
    }
}
