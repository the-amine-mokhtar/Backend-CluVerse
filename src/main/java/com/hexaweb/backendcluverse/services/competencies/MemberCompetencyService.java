package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchCandidateResponse;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchingRequest;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencyMatchingResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyGapResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyRequest;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyResponse;
import com.hexaweb.backendcluverse.dto.Competencies.MemberCompetencyUpdateRequest;
import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.entities.competencies.MemberCompetency;
import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import com.hexaweb.backendcluverse.repositories.Competencies.CompetencyRepository;
import com.hexaweb.backendcluverse.repositories.Competencies.MemberCompetencyRepository;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public List<MemberCompetencyResponse> getByUser(Long userId) {
        return memberCompetencyRepository.findResponsesByUserId(userId);
    }

    public List<MemberCompetencyResponse> getByClub(Long clubId) {
        return memberCompetencyRepository.findResponsesByClubId(clubId);
    }

    public MemberCompetencyResponse getById(Long id) {
        return memberCompetencyRepository.findResponseById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));
    }

    public MemberCompetencyResponse create(MemberCompetencyRequest request) {
        validateLevels(request.getCurrentLevel(), request.getTargetLevel());
        validateSkillExists(request.getSkillId());
        validateUniqueness(request.getUserId(), request.getSkillId(), null);

        MemberCompetency entity = new MemberCompetency();
        entity.setUserId(request.getUserId());
        entity.setSkillId(request.getSkillId());
        entity.setCurrentLevel(request.getCurrentLevel());
        entity.setTargetLevel(request.getTargetLevel());
        entity.setPreviousLevel(request.getCurrentLevel());
        entity.setEndorsementCount(0);
        entity.setLastUpdatedBy(UpdateSource.MANUAL);

        MemberCompetency saved = memberCompetencyRepository.save(entity);
        log.info("Created member competency id={} for userId={} skillId={}", saved.getId(), saved.getUserId(), saved.getSkillId());
        return getById(saved.getId());
    }

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
        return getById(saved.getId());
    }

    public void delete(Long id) {
        if (!memberCompetencyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found");
        }

        memberCompetencyRepository.deleteById(id);
        log.info("Deleted member competency id={}", id);
    }

    public MemberCompetencyResponse endorse(Long id) {
        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        int nextCount = existing.getEndorsementCount() == null ? 1 : existing.getEndorsementCount() + 1;
        existing.setEndorsementCount(nextCount);
        existing.setLastUpdatedBy(UpdateSource.PEER_ENDORSEMENT);

        MemberCompetency saved = memberCompetencyRepository.save(existing);
        log.info("Endorsed member competency id={} endorsementCount={}", saved.getId(), saved.getEndorsementCount());
        return getById(saved.getId());
    }

    public MemberCompetencyGapResponse getGap(Long id) {
        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        int gap = existing.getTargetLevel() - existing.getCurrentLevel();
        return new MemberCompetencyGapResponse(existing.getId(), existing.getCurrentLevel(), existing.getTargetLevel(), gap);
    }

    public MemberCompetencyResponse applySpeechAnalyzerScore(Long id, Integer speechScore) {
        if (speechScore == null || speechScore < 0 || speechScore > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Speech score must be between 0 and 100");
        }

        MemberCompetency existing = memberCompetencyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member competency not found"));

        existing.setPreviousLevel(existing.getCurrentLevel());
        existing.setCurrentLevel(speechScore);
        existing.setLastUpdatedBy(UpdateSource.SPEECH_ANALYZER);

        MemberCompetency saved = memberCompetencyRepository.save(existing);
        log.info("Applied speech analyzer score={} on member competency id={}", speechScore, saved.getId());
        return getById(saved.getId());
    }

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

        List<MemberCompetencyResponse> byClub = memberCompetencyRepository.findResponsesByClubId(request.getClubId());
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
            int gap = Math.max(memberSkill.getGap() == null
                ? memberSkill.getTargetLevel() - memberSkill.getCurrentLevel()
                : memberSkill.getGap(), 0);
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

        if (currentLevel < 0 || currentLevel > 100 || targetLevel < 0 || targetLevel > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Levels must be between 0 and 100");
        }
    }
}
