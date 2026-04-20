package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.dto.Competencies.BulkTargetRequest;
import com.hexaweb.backendcluverse.dto.Competencies.ClubCompetencyStats;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchCandidateResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchingRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchingResponse;
import com.hexaweb.backendcluverse.dto.Competencies.LevelUpdateRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyGapResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyUpdateRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.competencies.Competency;
import com.hexaweb.backendcluverse.entities.competencies.MemberCompetency;
import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencyRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.MemberCompetencyRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberCompetencyService {

    private final MemberCompetencyRepository memberCompetencyRepository;
    private final CompetencyRepository competencyRepository;
    private final UserRepository userRepository;
    private final BadgeEvaluationService badgeEvaluationService;

    @Transactional(readOnly = true)
    public List<MemberCompetencyResponse> getByUser(Long userId) {
        List<MemberCompetency> records = memberCompetencyRepository.findByUserId(userId);
        return enrichResponses(records);
    }

    @Transactional(readOnly = true)
    public List<MemberCompetencyResponse> getByClub(Long clubId) {
        List<MemberCompetency> records = memberCompetencyRepository.findByClubId(clubId);
        return enrichResponses(records);
    }

    @Transactional(readOnly = true)
    public MemberCompetencyResponse getById(Long id) {
        MemberCompetency entity = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));
        return enrichResponse(entity, buildUserMap(Set.of(entity.getUserId())), buildCompetencyMap(Set.of(entity.getSkillId())));
    }

    @Transactional
    public MemberCompetencyResponse create(MemberCompetencyRequest request) {
        return assign(request);
    }

    @Transactional
    public MemberCompetencyResponse assign(MemberCompetencyRequest request) {
        Long competencyId = resolveCompetencyId(request.getSkillId(), request.getCompetencyId());
        validateLevels(request.getCurrentLevel(), request.getTargetLevel());
        validateSkillExists(competencyId);
        validateUniqueness(request.getUserId(), competencyId, null);

        MemberCompetency entity = new MemberCompetency();
        entity.setUserId(request.getUserId());
        entity.setSkillId(competencyId);
        entity.setCurrentLevel(request.getCurrentLevel());
        entity.setTargetLevel(request.getTargetLevel());
        entity.setPreviousLevel(0);
        entity.setEndorsementCount(0);
        entity.setLastUpdatedBy(request.getLastUpdatedBy() == null ? UpdateSource.MANUAL : request.getLastUpdatedBy());

        MemberCompetency saved = memberCompetencyRepository.save(entity);
        log.info("Created member competency id={} for userId={} skillId={}", saved.getId(), saved.getUserId(), saved.getSkillId());
        badgeEvaluationService.checkAndAward(saved.getUserId(), saved.getLastUpdatedBy());
        return getById(saved.getId());
    }

    @Transactional
    public MemberCompetencyResponse update(Long id, MemberCompetencyUpdateRequest request) {
        validateLevels(request.getCurrentLevel(), request.getTargetLevel());

        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        existing.setPreviousLevel(existing.getCurrentLevel());
        existing.setCurrentLevel(request.getCurrentLevel());
        existing.setTargetLevel(request.getTargetLevel());
        existing.setLastUpdatedBy(UpdateSource.MANUAL);

        MemberCompetency saved = memberCompetencyRepository.save(existing);
        log.info("Updated member competency id={}", saved.getId());
        badgeEvaluationService.checkAndAward(saved.getUserId(), saved.getLastUpdatedBy());
        return getById(saved.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (!memberCompetencyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found");
        }

        memberCompetencyRepository.deleteById(id);
        log.info("Deleted member competency id={}", id);
    }

    @Transactional
    public MemberCompetencyResponse endorse(Long id) {
        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));
        return addEndorsement(existing.getUserId(), existing.getSkillId());
    }

    @Transactional(readOnly = true)
    public MemberCompetencyGapResponse getGap(Long id) {
        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        int gap = existing.getTargetLevel() - existing.getCurrentLevel();
        return new MemberCompetencyGapResponse(existing.getId(), existing.getCurrentLevel(), existing.getTargetLevel(), gap);
    }

    @Transactional
    public MemberCompetencyResponse applySpeechAnalyzerScore(Long id, Integer speechScore) {
        if (speechScore == null || speechScore < 0 || speechScore > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Speech score must be between 0 and 100");
        }

        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        existing.setPreviousLevel(existing.getCurrentLevel());
        int mappedLevel = Math.max(0, Math.min(5, Math.round(speechScore / 20.0f)));
        existing.setCurrentLevel(mappedLevel);
        existing.setLastUpdatedBy(UpdateSource.SPEECH_ANALYZER);

        MemberCompetency saved = memberCompetencyRepository.save(existing);
        log.info("Applied speech analyzer score={} mappedLevel={} on member competency id={}", speechScore, mappedLevel, saved.getId());
        badgeEvaluationService.checkAndAward(saved.getUserId(), UpdateSource.SPEECH_ANALYZER);
        return getById(saved.getId());
    }

    @Transactional
    public MemberCompetencyResponse updateLevel(Long userId, Long competencyId, LevelUpdateRequest request) {
        if (request.getNewLevel() == null || request.getNewLevel() < 0 || request.getNewLevel() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "newLevel must be between 0 and 5");
        }

        MemberCompetency mc = memberCompetencyRepository.findByUserIdAndSkillId(userId, competencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        int oldLevel = mc.getCurrentLevel();
        mc.setPreviousLevel(oldLevel);
        mc.setCurrentLevel(request.getNewLevel());
        mc.setLastUpdatedBy(request.getSource() == null ? UpdateSource.MANUAL : request.getSource());

        MemberCompetency saved = memberCompetencyRepository.save(mc);
        log.info("Updated level userId={} competencyId={} : {} -> {}", userId, competencyId, oldLevel, request.getNewLevel());
        badgeEvaluationService.checkAndAward(userId, saved.getLastUpdatedBy());
        return getById(saved.getId());
    }

    @Transactional
    public int bulkSetTarget(BulkTargetRequest request) {
        Long competencyId = resolveCompetencyId(request.getSkillId(), request.getCompetencyId());
        if (request.getTargetLevel() == null || request.getTargetLevel() < 0 || request.getTargetLevel() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetLevel must be between 0 and 5");
        }

        int updated = 0;
        for (Long userId : request.getUserIds()) {
            MemberCompetency mc = memberCompetencyRepository.findByUserIdAndSkillId(userId, competencyId).orElse(null);
            if (mc == null) {
                continue;
            }
            mc.setTargetLevel(request.getTargetLevel());
            mc.setLastUpdatedBy(UpdateSource.MANUAL);
            memberCompetencyRepository.save(mc);
            updated++;
        }

        log.info("Bulk target set: {} members updated for competencyId={}", updated, competencyId);
        return updated;
    }

    @Transactional
    public MemberCompetencyResponse addEndorsement(Long userId, Long competencyId) {
        MemberCompetency mc = memberCompetencyRepository.findByUserIdAndSkillId(userId, competencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        int nextCount = mc.getEndorsementCount() == null ? 1 : mc.getEndorsementCount() + 1;
        mc.setEndorsementCount(nextCount);
        mc.setLastUpdatedBy(UpdateSource.PEER_ENDORSEMENT);

        if (nextCount % 3 == 0 && mc.getCurrentLevel() < mc.getTargetLevel()) {
            mc.setPreviousLevel(mc.getCurrentLevel());
            mc.setCurrentLevel(mc.getCurrentLevel() + 1);
            log.info("Auto level-up via endorsement userId={} competencyId={}", userId, competencyId);
        }

        MemberCompetency saved = memberCompetencyRepository.save(mc);
        badgeEvaluationService.checkAndAward(userId, UpdateSource.PEER_ENDORSEMENT);
        return getById(saved.getId());
    }

    @Transactional(readOnly = true)
    public ClubCompetencyStats getStats(Long clubId) {
        List<MemberCompetency> all = memberCompetencyRepository.findByClubId(clubId);

        double avgLevel = all.stream()
                .mapToInt(MemberCompetency::getCurrentLevel)
                .average()
                .orElse(0.0);

        List<MemberCompetency> gaps = all.stream()
                .filter(mc -> mc.getCurrentLevel() < mc.getTargetLevel())
                .sorted(Comparator.comparingInt(mc -> (mc.getTargetLevel() - mc.getCurrentLevel())))
                .toList();

        int totalMembers = (int) all.stream()
                .map(MemberCompetency::getUserId)
                .distinct()
                .count();

        int criticalGaps = (int) gaps.stream()
                .filter(mc -> (mc.getTargetLevel() - mc.getCurrentLevel()) >= 2)
                .count();

        return ClubCompetencyStats.builder()
                .totalMembers(totalMembers)
                .avgLevelAcrossAll(Math.round(avgLevel * 10.0) / 10.0)
                .totalGaps(gaps.size())
                .criticalGaps(criticalGaps)
                .build();
    }

    @Transactional(readOnly = true)
    public CompetencyMatchingResponse getSmartMatching(CompetencyMatchingRequest request) {
        if (request.getRequiredSkillIds() == null || request.getRequiredSkillIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one required skill is needed");
        }

        Set<Long> requiredSkillIds = new LinkedHashSet<>(request.getRequiredSkillIds());
        Map<Long, String> requiredSkillNames = competencyRepository.findAllById(requiredSkillIds).stream()
            .collect(Collectors.toMap(
                competency -> competency.getId(),
                competency -> competency.getName(),
                (left, right) -> left,
                HashMap::new
            ));

        if (requiredSkillNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid required skills were found");
        }

        List<MemberCompetencyResponse> byClub = getByClub(request.getClubId());
        Map<Long, List<MemberCompetencyResponse>> byUser = byClub.stream()
            .collect(Collectors.groupingBy(MemberCompetencyResponse::getUserId));

        Set<Long> userIds = byUser.keySet();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        List<CompetencyMatchCandidateResponse> ranked = new ArrayList<>();
        int requiredCount = requiredSkillNames.size();

        for (Map.Entry<Long, List<MemberCompetencyResponse>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            List<MemberCompetencyResponse> userSkills = entry.getValue();

            Map<Long, MemberCompetencyResponse> skillMap = userSkills.stream()
                .collect(Collectors.toMap(
                    MemberCompetencyResponse::getSkillId,
                    item -> item,
                    (left, right) -> left
                ));

            int matchedSkills = 0;
            double sumScore = 0.0;
            double sumGap = 0.0;
            List<String> missingSkills = new ArrayList<>();

            for (Map.Entry<Long, String> required : requiredSkillNames.entrySet()) {
            Long skillId = required.getKey();
            MemberCompetencyResponse memberSkill = skillMap.get(skillId);

            if (memberSkill == null) {
                missingSkills.add(required.getValue());
                continue;
            }

            matchedSkills++;
            int gap = Math.max(memberSkill.getGapLevel() == null
                ? memberSkill.getTargetLevel() - memberSkill.getCurrentLevel()
                : memberSkill.getGapLevel(), 0);
            sumGap += gap;

            double levelProgress = memberSkill.getTargetLevel() == null || memberSkill.getTargetLevel() <= 0
                ? 0.5
                : Math.min((double) memberSkill.getCurrentLevel() / (double) memberSkill.getTargetLevel(), 1.0);
            double gapFactor = 1.0 - Math.min(gap / 100.0, 1.0);
            sumScore += (levelProgress * 0.65) + (gapFactor * 0.35);
            }

            double normalizedScore = requiredCount == 0 ? 0.0 : (sumScore / requiredCount) * 100.0;
            double missingPenalty = requiredCount == 0 ? 0.0 : ((double) missingSkills.size() / requiredCount) * 35.0;
            double finalScore = Math.max(0.0, normalizedScore - missingPenalty);
            double averageGap = matchedSkills == 0 ? 100.0 : sumGap / matchedSkills;

            String readiness = finalScore >= 75 ? "HIGH" : finalScore >= 50 ? "MEDIUM" : "LOW";
            User user = usersById.get(userId);
            String fullName = user == null
                ? "User #" + userId
                : ((user.getFirstName() == null ? "" : user.getFirstName()) + " " +
                (user.getLastName() == null ? "" : user.getLastName())).trim();
            if (fullName.isBlank()) {
            fullName = user != null && user.getEmail() != null ? user.getEmail() : "User #" + userId;
            }

            ranked.add(new CompetencyMatchCandidateResponse(
                userId,
                fullName,
                user == null ? "" : user.getEmail(),
                Math.round(finalScore * 10.0) / 10.0,
                readiness,
                matchedSkills,
                requiredCount,
                Math.round(averageGap * 10.0) / 10.0,
                missingSkills
            ));
        }

        int topN = request.getTopN() == null ? 5 : Math.max(1, Math.min(request.getTopN(), 20));
        List<CompetencyMatchCandidateResponse> recommendations = ranked.stream()
            .sorted(Comparator
                .comparing(CompetencyMatchCandidateResponse::getScore).reversed()
                .thenComparing(CompetencyMatchCandidateResponse::getMatchedSkills, Comparator.reverseOrder())
                .thenComparing(CompetencyMatchCandidateResponse::getAverageGap)
            )
            .limit(topN)
            .toList();

        return new CompetencyMatchingResponse(
            request.getContextType(),
            request.getContextTitle(),
            requiredCount,
            ranked.size(),
            recommendations
        );
    }

    private void validateUniqueness(Long userId, Long skillId, Long currentId) {
        boolean exists = currentId == null
                ? memberCompetencyRepository.existsByUserIdAndSkillId(userId, skillId)
                : memberCompetencyRepository.existsByUserIdAndSkillIdAndIdNot(userId, skillId, currentId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member competency already exists for this user and skill");
        }
    }

    private void validateSkillExists(Long skillId) {
        if (!competencyRepository.existsById(skillId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competency does not exist");
        }
    }

    private void validateLevels(Integer currentLevel, Integer targetLevel) {
        if (currentLevel == null || targetLevel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Levels are required");
        }

        if (currentLevel < 0 || currentLevel > 5 || targetLevel < 0 || targetLevel > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Levels must be between 0 and 5");
        }
    }

    private Long resolveCompetencyId(Long skillId, Long competencyId) {
        Long value = competencyId != null ? competencyId : skillId;
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "competencyId is required");
        }
        return value;
    }

    private List<MemberCompetencyResponse> enrichResponses(List<MemberCompetency> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = records.stream().map(MemberCompetency::getUserId).collect(Collectors.toSet());
        Set<Long> competencyIds = records.stream().map(MemberCompetency::getSkillId).collect(Collectors.toSet());

        Map<Long, User> userMap = buildUserMap(userIds);
        Map<Long, Competency> competencyMap = buildCompetencyMap(competencyIds);

        return records.stream()
                .map(mc -> enrichResponse(mc, userMap, competencyMap))
                .toList();
    }

    private MemberCompetencyResponse enrichResponse(MemberCompetency mc,
                                                    Map<Long, User> userMap,
                                                    Map<Long, Competency> competencyMap) {
        MemberCompetencyResponse response = new MemberCompetencyResponse();
        response.setId(mc.getId());
        response.setUserId(mc.getUserId());
        response.setSkillId(mc.getSkillId());
        response.setCompetencyId(mc.getSkillId());
        response.setCurrentLevel(mc.getCurrentLevel());
        response.setTargetLevel(mc.getTargetLevel());
        response.setPreviousLevel(mc.getPreviousLevel());
        response.setEndorsementCount(mc.getEndorsementCount());
        response.setLastUpdatedBy(mc.getLastUpdatedBy());
        response.setLastUpdated(mc.getLastUpdated());

        int gap = mc.getTargetLevel() - mc.getCurrentLevel();
        response.setGap(gap);
        response.setGapLevel(gap);

        User user = userMap.get(mc.getUserId());
        if (user != null) {
            String firstName = user.getFirstName() == null ? "" : user.getFirstName();
            String lastName = user.getLastName() == null ? "" : user.getLastName();
            String fullName = (firstName + " " + lastName).trim();
            response.setUserName(fullName.isBlank() ? user.getEmail() : fullName);
        }

        Competency competency = competencyMap.get(mc.getSkillId());
        if (competency != null) {
            response.setSkillName(competency.getName());
            response.setCompetencyName(competency.getName());
            response.setCategory(competency.getCategory());
            response.setCompetencyCategory(competency.getCategory() == null ? null : competency.getCategory().name());
        }

        return response;
    }

    private Map<Long, User> buildUserMap(Set<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private Map<Long, Competency> buildCompetencyMap(Set<Long> competencyIds) {
        return competencyRepository.findAllById(competencyIds).stream()
                .collect(Collectors.toMap(Competency::getId, competency -> competency));
    }
}
