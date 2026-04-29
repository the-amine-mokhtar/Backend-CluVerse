package com.hexaweb.backendcluverse.services.competencies;

import com.hexaweb.backendcluverse.enumerations.UpdateSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BadgeEvaluationService {

    // Placeholder integration point for badge awarding workflow.
    public void checkAndAward(Long userId, UpdateSource source) {
        log.debug("Badge evaluation triggered for userId={} source={}", userId, source);
    }
}
