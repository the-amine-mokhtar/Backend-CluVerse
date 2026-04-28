package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.*;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.event.EventParticipant;
import com.hexaweb.backendcluverse.enumerations.EventStatus;
import com.hexaweb.backendcluverse.enumerations.ParticipationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class EventAiService {

    private final EventService eventService;
    private final EventParticipantService eventParticipantService;
    private final Map<Long, CachedDashboard<EventAiDashboardDto>> organizerCache = new ConcurrentHashMap<>();
    private final Map<Long, CachedDashboard<EventAiSchedulingDto>> schedulingCache = new ConcurrentHashMap<>();
    private final Map<String, CachedDashboard<ParticipantAiDashboardDto>> participantCache = new ConcurrentHashMap<>();

    @Value("${app.ai.cache.ttl-seconds:300}")
    private long cacheTtlSeconds;

    @Value("${app.ai.organizer.weights.popularity:0.35}")
    private double organizerPopularityWeight;
    @Value("${app.ai.organizer.weights.trend:0.25}")
    private double organizerTrendWeight;
    @Value("${app.ai.organizer.weights.urgency:0.20}")
    private double organizerUrgencyWeight;
    @Value("${app.ai.organizer.weights.conversion:0.20}")
    private double organizerConversionWeight;

    @Value("${app.ai.participant.weights.popularity:0.30}")
    private double participantPopularityWeight;
    @Value("${app.ai.participant.weights.trend:0.25}")
    private double participantTrendWeight;
    @Value("${app.ai.participant.weights.urgency:0.20}")
    private double participantUrgencyWeight;
    @Value("${app.ai.participant.weights.affinity:0.25}")
    private double participantAffinityWeight;

    public EventAiService(EventService eventService, EventParticipantService eventParticipantService) {
        this.eventService = eventService;
        this.eventParticipantService = eventParticipantService;
    }

    public EventAiDashboardDto buildOrganizerDashboard(Long clubId) {
        if (clubId == null) {
            return emptyOrganizerDashboard();
        }

        CachedDashboard<EventAiDashboardDto> cached = organizerCache.get(clubId);
        if (isCacheFresh(cached)) {
            return cached.payload();
        }

        List<EventAiRankedEventDto> ranked = rankOrganizerEvents(eventService.findByClubId(clubId));
        List<EventAiRankedEventDto> active = ranked.stream()
                .filter(event -> !isArchivedStatus(event.getStatus()))
                .toList();

        int avgScore = active.isEmpty()
                ? 0
                : round(active.stream().mapToInt(event -> event.getAiInsight().getScore()).average().orElse(0));

        int trending = (int) active.stream().filter(event -> "hot".equals(event.getAiInsight().getMomentum())).count();
        int popular = (int) active.stream().filter(event -> event.getAiInsight().getPopularityScore() >= 70).count();
        int almostFull = (int) active.stream().filter(event -> "Almost Full".equals(event.getAiInsight().getBadgeLabel())).count();

        int totalRegistrations = active.stream()
                .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .sum();
        int totalSeats = active.stream()
                .mapToInt(event -> Optional.ofNullable(event.getAvailableSeats()).orElse(0))
                .sum();
        int avgParticipation = active.isEmpty()
                ? 0
                : round(active.stream()
                        .mapToInt(event -> computeParticipationScore(
                                Optional.ofNullable(event.getParticipantsCount()).orElse(0),
                                Optional.ofNullable(event.getCapacity()).orElse(0)))
                        .average()
                        .orElse(0));

        List<AiStatisticDto> statistics = List.of(
                AiStatisticDto.builder().label("Total Events").value(String.valueOf(active.size())).hint("Active events analysed by the backend IA").build(),
                AiStatisticDto.builder().label("Registrations").value(String.valueOf(totalRegistrations)).hint("Confirmed participants used in the score").build(),
                AiStatisticDto.builder().label("Avg Participation").value(avgParticipation + "%").hint("Average fill rate across active events").build(),
                AiStatisticDto.builder().label("Seats Available").value(String.valueOf(totalSeats)).hint("Remaining capacity still available").build()
        );

        EventAiDashboardDto dashboard = EventAiDashboardDto.builder()
                .avgScore(avgScore)
                .trending(trending)
                .popular(popular)
                .almostFull(almostFull)
                .statistics(statistics)
                .rankedEvents(ranked)
                .highlights(active.stream().limit(4).toList())
                .trendingEvents(active.stream().filter(event -> "hot".equals(event.getAiInsight().getMomentum())).limit(3).toList())
                .almostFullEvents(active.stream().filter(event -> "Almost Full".equals(event.getAiInsight().getBadgeLabel())).limit(3).toList())
                .build();

        organizerCache.put(clubId, new CachedDashboard<>(dashboard, LocalDateTime.now()));
        return dashboard;
    }

    public EventAiSchedulingDto buildOrganizerSchedulingOptimizer(Long clubId) {
        if (clubId == null) {
            return emptyOrganizerScheduling();
        }

        CachedDashboard<EventAiSchedulingDto> cached = schedulingCache.get(clubId);
        if (isCacheFresh(cached)) {
            return cached.payload();
        }

        List<EventAiRankedEventDto> ranked = rankOrganizerEvents(eventService.findByClubId(clubId));
        List<EventAiRankedEventDto> history = ranked.stream()
                .filter(this::isPastEvent)
                .toList();

        if (history.isEmpty()) {
            EventAiSchedulingDto empty = emptyOrganizerScheduling();
            schedulingCache.put(clubId, new CachedDashboard<>(empty, LocalDateTime.now()));
            return empty;
        }

        List<EventAiSchedulingSlotDto> topDays = aggregateSchedulingSlots(history, "day").stream().limit(4).toList();
        List<EventAiSchedulingSlotDto> topHours = aggregateSchedulingSlots(history, "hour").stream().limit(4).toList();
        List<EventAiSchedulingSlotDto> topMonths = aggregateSchedulingSlots(history, "month");
        List<EventAiSchedulingTrendPointDto> historicalTrend = buildHistoricalTrend(history);
        List<EventAiSchedulingTrendPointDto> forecastTrend = buildForecastTrend(history, historicalTrend);
        List<EventAiSchedulingTrendPointDto> monthlyTrend = buildMonthlyTrend(history);

        EventAiSchedulingSlotDto bestDay = topDays.isEmpty() ? null : topDays.get(0);
        EventAiSchedulingSlotDto bestHour = topHours.isEmpty() ? null : topHours.get(0);
        EventAiSchedulingSlotDto bestMonth = topMonths.isEmpty() ? null : topMonths.get(0);

        int baselineParticipants = round(history.stream()
                .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .average()
                .orElse(0));

        int predictionParticipants = Math.max(
                baselineParticipants,
                baselineParticipants
                        + (bestDay != null ? round(bestDay.getParticipants() * 0.18) : 0)
                        + (bestHour != null ? round(bestHour.getParticipants() * 0.12) : 0)
        );

        int expectedLift = baselineParticipants <= 0
                ? 0
                : Math.max(0, round(((predictionParticipants - baselineParticipants) / (double) baselineParticipants) * 100));

        String trendDirection = computeTrendDirection(historicalTrend, forecastTrend);
        int trendDeltaPercent = computeTrendDeltaPercent(historicalTrend, forecastTrend);
        int successProbability = computeSchedulingSuccessProbability(confidenceFor(history.size()), expectedLift, bestDay, bestHour, trendDirection);

        EventAiSchedulingDto scheduling = EventAiSchedulingDto.builder()
                .recommendationTitle(buildSchedulingRecommendationTitle(bestDay, bestHour))
                .recommendationNarrative(buildSchedulingNarrative(bestDay, bestHour))
                .forecastNarrative(buildForecastNarrative(trendDirection, trendDeltaPercent, bestDay, bestHour))
                .predictionParticipants(predictionParticipants)
                .expectedLift(expectedLift)
                .confidence(confidenceFor(history.size()))
                .successProbability(successProbability)
                .trendDirection(trendDirection)
                .trendDeltaPercent(trendDeltaPercent)
                .analyzedPastEvents(history.size())
                .bestDay(bestDay)
                .bestHour(bestHour)
                .bestMonth(bestMonth)
                .topDays(topDays)
                .topHours(topHours)
                .historicalTrend(historicalTrend)
                .forecastTrend(forecastTrend)
                .monthlyTrend(monthlyTrend)
                .forecastHighlights(buildForecastHighlights(trendDirection, trendDeltaPercent, bestDay, bestHour, bestMonth))
                .build();

        schedulingCache.put(clubId, new CachedDashboard<>(scheduling, LocalDateTime.now()));
        return scheduling;
    }

    public ParticipantAiDashboardDto buildParticipantDashboard(Long userId, Long clubId) {
        if (userId == null || clubId == null) {
            return emptyParticipantDashboard();
        }

        String cacheKey = userId + ":" + clubId;
        CachedDashboard<ParticipantAiDashboardDto> cached = participantCache.get(cacheKey);
        if (isCacheFresh(cached)) {
            return cached.payload();
        }

        List<Event> accessibleEvents = eventService.getAllAccessibleEvents(clubId);
        List<EventParticipant> myParticipations = eventParticipantService.findByUserId(userId);

        Set<Long> joinedEventIds = myParticipations.stream()
                .filter(participation -> participation.getStatus() != ParticipationStatus.CANCELLED)
                .map(participation -> participation.getEvent().getId())
                .collect(Collectors.toSet());

        PreferenceProfile profile = buildPreferenceProfile(myParticipations);
        List<ParticipantAiRecommendationDto> recommendations = rankParticipantEvents(
                accessibleEvents.stream()
                        .filter(event -> event.getStatus() != EventStatus.CANCELLED)
                        .filter(event -> event.getStatus() != EventStatus.COMPLETED)
                        .filter(event -> !joinedEventIds.contains(event.getId()))
                        .toList(),
                profile
        );

        int recommended = (int) recommendations.stream()
                .filter(event -> event.getParticipationAi().getBadges().contains("Recommended"))
                .count();
        int trending = (int) recommendations.stream()
                .filter(event -> event.getParticipationAi().getBadges().contains("Trending"))
                .count();
        int urgent = (int) recommendations.stream()
                .filter(event -> !"low".equals(event.getParticipationAi().getUrgencyLevel()))
                .count();
        int avgAffinity = recommendations.isEmpty()
                ? 0
                : round(recommendations.stream().mapToInt(event -> event.getParticipationAi().getAffinityScore()).average().orElse(0));
        int totalRegistrations = recommendations.stream()
                .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .sum();
        int totalOpenSeats = recommendations.stream()
                .filter(event -> event.getAvailableSeats() != null && event.getAvailableSeats() != Integer.MAX_VALUE)
                .mapToInt(event -> Optional.ofNullable(event.getAvailableSeats()).orElse(0))
                .sum();

        List<AiStatisticDto> statistics = List.of(
                AiStatisticDto.builder().label("Suggested Events").value(String.valueOf(recommendations.size())).hint("Events scored for the current participant").build(),
                AiStatisticDto.builder().label("Registrations").value(String.valueOf(totalRegistrations)).hint("Popularity signals used in recommendation").build(),
                AiStatisticDto.builder().label("Avg Affinity").value(avgAffinity + "%").hint("Estimated fit between user history and event mix").build(),
                AiStatisticDto.builder().label("Open Seats").value(String.valueOf(totalOpenSeats)).hint("Remaining seats across current suggestions").build()
        );

        ParticipantAiDashboardDto dashboard = ParticipantAiDashboardDto.builder()
                .recommended(recommended)
                .trending(trending)
                .urgent(urgent)
                .profileSignals(profile.preferredCategories().size())
                .profileSummary(buildProfileSummary(profile))
                .statistics(statistics)
                .recommendations(recommendations.stream().limit(4).toList())
                .trendingEvents(recommendations.stream()
                        .filter(event -> event.getParticipationAi().getBadges().contains("Trending"))
                        .limit(3)
                        .toList())
                .urgentEvents(recommendations.stream()
                        .filter(event -> !"low".equals(event.getParticipationAi().getUrgencyLevel()))
                        .limit(3)
                        .toList())
                .build();

        participantCache.put(cacheKey, new CachedDashboard<>(dashboard, LocalDateTime.now()));
        return dashboard;
    }

    private List<EventAiRankedEventDto> rankOrganizerEvents(List<Event> events) {
        Map<String, Double> weights = organizerWeightMap();
        int participantsMax = events.stream()
                .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .max()
                .orElse(0);

        return events.stream()
                .map(event -> {
                    int participants = Optional.ofNullable(event.getParticipantsCount()).orElse(0);
                    int capacity = Optional.ofNullable(event.getCapacity()).orElse(0);
                    int popularityScore = normalize(participants, participantsMax);
                    int participationScore = computeParticipationScore(participants, capacity);
                    int recencyScore = computeUpcomingRecencyScore(event.getStartDate());
                    int trendScore = computeDemandTrendScore(participants, event.getStartDate());
                    int conversionScore = computeConversionScore(participationScore, trendScore, capacity);
                    int urgencyScore = computeOrganizerUrgencyScore(event);
                    int score = computeWeightedScore(Map.of(
                            "Popularity", popularityScore,
                            "Trend", trendScore,
                            "Urgency", urgencyScore,
                            "Conversion", conversionScore
                    ), weights);
                    String momentum = computeMomentum(trendScore);
                    String badgeLabel = resolveOrganizerBadge(participationScore, trendScore, popularityScore, event);

                    EventAiInsightDto insight = EventAiInsightDto.builder()
                            .eventId(event.getId())
                            .score(score)
                            .popularityScore(popularityScore)
                            .participationScore(participationScore)
                            .recencyScore(recencyScore)
                            .trendScore(trendScore)
                            .conversionScore(conversionScore)
                            .momentum(momentum)
                            .badgeLabel(badgeLabel)
                            .reasons(buildOrganizerReasons(event, popularityScore, trendScore, urgencyScore, conversionScore, recencyScore))
                            .build();

                    return mapOrganizerEvent(event, insight);
                })
                .sorted(Comparator.comparingInt((EventAiRankedEventDto dto) -> dto.getAiInsight().getScore()).reversed())
                .toList();
    }

    private List<ParticipantAiRecommendationDto> rankParticipantEvents(List<Event> events, PreferenceProfile profile) {
        Map<String, Double> weights = participantWeightMap();
        boolean profileColdStart = profile.isColdStart();
        int participantsMax = events.stream()
                .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .max()
                .orElse(0);

        return events.stream()
                .filter(event -> event.getStatus() != EventStatus.COMPLETED)
                .map(event -> {
                    int participants = Optional.ofNullable(event.getParticipantsCount()).orElse(0);
                    int popularityScore = normalize(participants, participantsMax);
                    int availabilityScore = computeAvailabilityScore(event);
                    int recencyScore = computeUpcomingRecencyScore(event.getStartDate());
                    int trendScore = computeDemandTrendScore(participants, event.getStartDate());
                    int affinityScore = computeAffinityScore(event, profile);
                    int score = computeWeightedScore(Map.of(
                            "Popularity", popularityScore,
                            "Trend", trendScore,
                            "Urgency", availabilityScore,
                            "Affinity", affinityScore
                    ), weights);

                    String urgencyLevel = computeUrgencyLevel(event, availabilityScore);
                    String recommendationLabel = resolveParticipantLabel(score, trendScore, affinityScore, event, profileColdStart);

                    ParticipantAiInsightDto insight = ParticipantAiInsightDto.builder()
                            .eventId(event.getId())
                            .score(score)
                            .popularityScore(popularityScore)
                            .availabilityScore(availabilityScore)
                            .recencyScore(recencyScore)
                            .trendScore(trendScore)
                            .affinityScore(affinityScore)
                            .urgencyLevel(urgencyLevel)
                            .recommendationLabel(recommendationLabel)
                            .explanation(buildParticipantExplanation(event, trendScore, affinityScore, availabilityScore, profileColdStart))
                            .decisionMessage(buildDecisionMessage(score, event, trendScore, affinityScore, availabilityScore, profileColdStart))
                            .seatsLeft(extractSeatsLeft(event))
                            .badges(buildParticipantBadges(score, event, trendScore, affinityScore, availabilityScore, profileColdStart))
                            .build();

                    return mapParticipantEvent(event, insight);
                })
                .sorted(Comparator.comparingInt((ParticipantAiRecommendationDto dto) -> dto.getParticipationAi().getScore()).reversed())
                .toList();
    }

    private EventAiRankedEventDto mapOrganizerEvent(Event event, EventAiInsightDto insight) {
        return EventAiRankedEventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .category(event.getCategory())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .participantsCount(Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .capacity(event.getCapacity())
                .availableSeats(normalizeAvailableSeats(event))
                .locationName(event.getLocationName())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .meetingUrl(event.getMeetingUrl())
                .campaignId(event.getCampaign() != null ? event.getCampaign().getId() : null)
                .campaignTitle(event.getCampaign() != null ? event.getCampaign().getTitle() : null)
                .aiInsight(insight)
                .build();
    }

    private ParticipantAiRecommendationDto mapParticipantEvent(Event event, ParticipantAiInsightDto insight) {
        return ParticipantAiRecommendationDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .status(event.getStatus() != null ? event.getStatus().name() : null)
                .category(event.getCategory())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .participantsCount(Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                .capacity(event.getCapacity())
                .availableSeats(normalizeAvailableSeats(event))
                .locationName(event.getLocationName())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .meetingUrl(event.getMeetingUrl())
                .campaignId(event.getCampaign() != null ? event.getCampaign().getId() : null)
                .campaignTitle(event.getCampaign() != null ? event.getCampaign().getTitle() : null)
                .participationAi(insight)
                .build();
    }

    private List<String> buildOrganizerReasons(Event event, int popularityScore, int trendScore, int urgencyScore, int conversionScore, int recencyScore) {
        List<String> reasons = new ArrayList<>();
        if (trendScore >= 78) reasons.add("Top factor: trend growth");
        if (urgencyScore >= 84) reasons.add("Top factor: limited seats");
        if (popularityScore >= 70) reasons.add("Top factor: strong popularity");
        if (conversionScore >= 70) reasons.add("Top factor: healthy conversion");
        if (recencyScore >= 82) reasons.add("Upcoming soon");
        if (event.getCampaign() != null) reasons.add("Campaign-linked event");
        if (Optional.ofNullable(event.getParticipantsCount()).orElse(0) == 0) {
            reasons.add("Cold-start estimate from recency and baseline demand");
        }
        if (reasons.isEmpty()) reasons.add("Stable activity");
        return reasons.stream().limit(3).toList();
    }

    private List<String> buildParticipantBadges(
            int score,
            Event event,
            int trendScore,
            int affinityScore,
            int urgencyScore,
            boolean profileColdStart
    ) {
        List<String> badges = new ArrayList<>();
        if (score >= 82 || affinityScore >= 72 || (profileColdStart && trendScore >= 72)) badges.add("Recommended");
        if (trendScore >= 74) badges.add("Trending");
        if (score >= 76 || urgencyScore >= 84) badges.add("Popular");
        if (isAlmostFull(event)) badges.add("Almost Full");
        if (affinityScore >= 66 && !profileColdStart) badges.add("Profile Match");
        if ("high".equals(computeUrgencyLevel(event, urgencyScore))) badges.add("Urgent");
        return badges.stream().limit(3).toList();
    }

    private String buildParticipantExplanation(
            Event event,
            int trendScore,
            int affinityScore,
            int urgencyScore,
            boolean profileColdStart
    ) {
        Integer seatsLeft = extractSeatsLeft(event);
        if (profileColdStart && affinityScore <= 35) {
            if (trendScore >= 72 || urgencyScore >= 84) {
                return "Cold-start mode: selected using global trend and urgency signals";
            }
            return "Cold-start mode: selected from global popularity and date proximity";
        }
        if (seatsLeft != null && seatsLeft > 0 && seatsLeft <= 3) {
            return seatsLeft == 1
                    ? "Only one seat is left, so timing matters"
                    : "Limited seats remain, so early participation is recommended";
        }
        if (affinityScore >= 66 && trendScore >= 74) {
            return "This event matches the participant profile and is gaining momentum";
        }
        if (affinityScore >= 66) {
            return "This event aligns with categories and campaigns already joined";
        }
        if (trendScore >= 74) {
            return "This event is attracting demand faster than average";
        }
        return "Balanced option based on timing, participation, and availability";
    }

    private String buildDecisionMessage(
            int score,
            Event event,
            int trendScore,
            int affinityScore,
            int urgencyScore,
            boolean profileColdStart
    ) {
        if (profileColdStart && trendScore >= 74) {
            return "Recommended from global trend signals while your profile is still learning";
        }
        if ((isAlmostFull(event) || urgencyScore >= 84) && trendScore >= 74) {
            return "High demand and limited seats: join soon";
        }
        if (isAlmostFull(event) || urgencyScore >= 84) {
            return "Almost full: early registration recommended";
        }
        if (score >= 84 && affinityScore >= 62) {
            return "Highly recommended for this participant profile";
        }
        if (trendScore >= 74) {
            return "Trending event with growing interest";
        }
        return "Relevant option based on current event signals";
    }

    private String buildProfileSummary(PreferenceProfile profile) {
        if (profile.isColdStart()) {
            return "Cold-start mode: recommendations are based on global trends and urgency until profile signals grow.";
        }
        return "Detected preferences: " + String.join(", ", profile.preferredCategories().stream().limit(3).toList())
                + ". Joined campaigns are also included in the recommendation logic.";
    }

    private PreferenceProfile buildPreferenceProfile(List<EventParticipant> participations) {
        List<String> preferredCategories = participations.stream()
                .filter(participation -> participation.getStatus() != ParticipationStatus.CANCELLED)
                .map(participation -> participation.getEvent().getCategory())
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        List<Long> joinedCampaignIds = participations.stream()
                .filter(participation -> participation.getStatus() != ParticipationStatus.CANCELLED)
                .map(participation -> participation.getEvent().getCampaign())
                .filter(Objects::nonNull)
                .map(campaign -> campaign.getId())
                .distinct()
                .toList();

        List<Integer> participationVolumes = participations.stream()
                .filter(participation -> participation.getStatus() != ParticipationStatus.CANCELLED)
                .map(participation -> Optional.ofNullable(participation.getEvent().getParticipantsCount()).orElse(0))
                .filter(value -> value > 0)
                .toList();

        int avgParticipationVolume = participationVolumes.isEmpty()
                ? 0
                : round(participationVolumes.stream().mapToInt(Integer::intValue).average().orElse(0));

        return new PreferenceProfile(preferredCategories, joinedCampaignIds, avgParticipationVolume);
    }

    private int computeParticipationScore(int participants, int capacity) {
        if (capacity <= 0) {
            return Math.min(100, participants * 8);
        }
        return Math.min(100, round(((double) participants / capacity) * 100));
    }

    private int computeAvailabilityScore(Event event) {
        Integer seatsLeft = extractSeatsLeft(event);
        if (seatsLeft == null) return 58;
        if (seatsLeft <= 1) return 100;
        if (seatsLeft <= 3) return 92;
        if (event.getCapacity() == null || event.getCapacity() <= 0) return 58;
        double ratio = seatsLeft / (double) event.getCapacity();
        if (ratio <= 0.10) return 84;
        if (ratio <= 0.25) return 68;
        if (ratio <= 0.50) return 52;
        return 36;
    }

    private int computeOrganizerUrgencyScore(Event event) {
        Integer seatsLeft = extractSeatsLeft(event);
        if (seatsLeft == null) return 40;
        if (seatsLeft <= 0) return 100;
        if (seatsLeft <= 1) return 95;
        if (seatsLeft <= 3) return 88;
        if (event.getCapacity() == null || event.getCapacity() <= 0) return 40;
        double ratio = seatsLeft / (double) event.getCapacity();
        if (ratio <= 0.10) return 76;
        if (ratio <= 0.25) return 62;
        if (ratio <= 0.50) return 46;
        return 30;
    }

    private int computeConversionScore(int participationScore, int trendScore, int capacity) {
        if (capacity <= 0) {
            return Math.min(92, trendScore + 8);
        }
        return round((participationScore * 0.65) + (trendScore * 0.35));
    }

    private int computeUpcomingRecencyScore(LocalDateTime startDate) {
        if (startDate == null) return 30;
        long daysUntil = Duration.between(LocalDateTime.now(), startDate).toDays();
        if (daysUntil < 0) return 10;
        if (daysUntil <= 1) return 100;
        if (daysUntil <= 3) return 92;
        if (daysUntil <= 7) return 82;
        if (daysUntil <= 14) return 68;
        if (daysUntil <= 30) return 52;
        return 34;
    }

    private int computeDemandTrendScore(int participants, LocalDateTime startDate) {
        if (startDate == null) {
            return participants > 0 ? 44 : 18;
        }
        long daysUntil = Math.max(1, Duration.between(LocalDateTime.now(), startDate).toDays());
        double demandRate = participants / (double) daysUntil;
        if (demandRate >= 8) return 100;
        if (demandRate >= 5) return 86;
        if (demandRate >= 3) return 72;
        if (demandRate >= 1.5) return 58;
        if (demandRate > 0) return 38;
        return 18;
    }

    private int computeAffinityScore(Event event, PreferenceProfile profile) {
        int score = 30;
        if (event.getCategory() != null && profile.preferredCategories().contains(event.getCategory().toUpperCase())) {
            score += 40;
        }
        if (event.getCampaign() != null && profile.joinedCampaignIds().contains(event.getCampaign().getId())) {
            score += 18;
        }
        int participants = Optional.ofNullable(event.getParticipantsCount()).orElse(0);
        if (profile.averageParticipationVolume() > 0) {
            int distance = Math.abs(participants - profile.averageParticipationVolume());
            if (distance <= 10) score += 12;
            else if (distance <= 25) score += 6;
        }
        return Math.min(100, score);
    }

    private int normalize(int value, int max) {
        if (max <= 0) {
            return value > 0 ? 55 : 18;
        }
        return Math.min(100, round(((double) value / max) * 100));
    }

    private boolean isPastEvent(EventAiRankedEventDto event) {
        return event.getStartDate() != null && event.getStartDate().isBefore(LocalDateTime.now());
    }

    private List<EventAiSchedulingSlotDto> aggregateSchedulingSlots(List<EventAiRankedEventDto> history, String mode) {
        Map<String, List<EventAiRankedEventDto>> grouped = history.stream()
                .filter(event -> event.getStartDate() != null)
                .collect(Collectors.groupingBy(event -> schedulingKey(event.getStartDate(), mode)));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<EventAiRankedEventDto> events = entry.getValue();
                    int participants = round(events.stream()
                            .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                            .average()
                            .orElse(0));
                    int fillRate = round(events.stream()
                            .mapToInt(event -> computeParticipationScore(
                                    Optional.ofNullable(event.getParticipantsCount()).orElse(0),
                                    Optional.ofNullable(event.getCapacity()).orElse(0)))
                            .average()
                            .orElse(0));
                    int aiScore = round(events.stream()
                            .mapToInt(event -> event.getAiInsight().getScore())
                            .average()
                            .orElse(0));
                    int score = Math.min(100, round((participants * 0.45) + (fillRate * 0.35) + (aiScore * 0.20) + Math.min(12, events.size() * 3)));

                    return EventAiSchedulingSlotDto.builder()
                            .label(schedulingLabel(entry.getKey(), mode))
                            .shortLabel(schedulingShortLabel(entry.getKey(), mode))
                            .score(score)
                            .participants(participants)
                            .fillRate(fillRate)
                            .sampleSize(events.size())
                            .build();
                })
                .sorted(Comparator
                        .comparingInt(EventAiSchedulingSlotDto::getScore).reversed()
                        .thenComparingInt(EventAiSchedulingSlotDto::getFillRate).reversed()
                        .thenComparingInt(EventAiSchedulingSlotDto::getParticipants).reversed())
                .toList();
    }

    private List<EventAiSchedulingTrendPointDto> buildMonthlyTrend(List<EventAiRankedEventDto> history) {
        Map<String, List<EventAiRankedEventDto>> grouped = history.stream()
                .filter(event -> event.getStartDate() != null)
                .collect(Collectors.groupingBy(event -> event.getStartDate().getYear() + "-" + event.getStartDate().getMonthValue(), TreeMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .skip(Math.max(0, grouped.size() - 6))
                .map(entry -> {
                    String[] parts = entry.getKey().split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    List<EventAiRankedEventDto> events = entry.getValue();
                    int participants = round(events.stream()
                            .mapToInt(event -> Optional.ofNullable(event.getParticipantsCount()).orElse(0))
                            .average()
                            .orElse(0));
                    int fillRate = round(events.stream()
                            .mapToInt(event -> computeParticipationScore(
                                    Optional.ofNullable(event.getParticipantsCount()).orElse(0),
                                    Optional.ofNullable(event.getCapacity()).orElse(0)))
                            .average()
                            .orElse(0));
                    int score = round(events.stream()
                            .mapToInt(event -> event.getAiInsight().getScore())
                            .average()
                            .orElse(0));

                    return EventAiSchedulingTrendPointDto.builder()
                            .label(monthShort(month) + " " + year)
                            .shortLabel(monthShort(month))
                            .participants(participants)
                            .fillRate(fillRate)
                            .score(score)
                            .projected(false)
                            .changeRate(0)
                            .build();
                })
                .toList();
    }

    private List<EventAiSchedulingTrendPointDto> buildHistoricalTrend(List<EventAiRankedEventDto> history) {
        List<EventAiRankedEventDto> sorted = history.stream()
                .filter(event -> event.getStartDate() != null)
                .sorted(Comparator.comparing(EventAiRankedEventDto::getStartDate))
                .toList();

        int startIndex = Math.max(0, sorted.size() - 8);
        List<EventAiRankedEventDto> sample = sorted.subList(startIndex, sorted.size());
        List<EventAiSchedulingTrendPointDto> points = new ArrayList<>();

        for (int i = 0; i < sample.size(); i++) {
            EventAiRankedEventDto event = sample.get(i);
            int participants = Optional.ofNullable(event.getParticipantsCount()).orElse(0);
            int fillRate = computeParticipationScore(participants, Optional.ofNullable(event.getCapacity()).orElse(0));
            int score = event.getAiInsight() != null ? event.getAiInsight().getScore() : fillRate;
            int previousParticipants = i == 0 ? participants : points.get(i - 1).getParticipants();
            int changeRate = previousParticipants <= 0
                    ? 0
                    : round(((participants - previousParticipants) / (double) previousParticipants) * 100);

            points.add(EventAiSchedulingTrendPointDto.builder()
                    .label(event.getStartDate().getDayOfMonth() + " " + monthShort(event.getStartDate().getMonthValue()))
                    .shortLabel(String.format("%02d/%02d", event.getStartDate().getDayOfMonth(), event.getStartDate().getMonthValue()))
                    .participants(participants)
                    .fillRate(fillRate)
                    .score(score)
                    .projected(false)
                    .changeRate(changeRate)
                    .build());
        }

        return points;
    }

    private List<EventAiSchedulingTrendPointDto> buildForecastTrend(
            List<EventAiRankedEventDto> history,
            List<EventAiSchedulingTrendPointDto> historicalTrend
    ) {
        if (historicalTrend.isEmpty()) {
            return List.of();
        }

        List<Integer> participantsHistory = historicalTrend.stream()
                .map(EventAiSchedulingTrendPointDto::getParticipants)
                .toList();
        List<Integer> fillRateHistory = historicalTrend.stream()
                .map(EventAiSchedulingTrendPointDto::getFillRate)
                .toList();
        List<Integer> scoreHistory = historicalTrend.stream()
                .map(EventAiSchedulingTrendPointDto::getScore)
                .toList();

        double slope = computeAverageDelta(participantsHistory);
        int movingAverageParticipants = round(lastValuesAverage(participantsHistory, 3));
        int movingAverageFillRate = round(lastValuesAverage(fillRateHistory, 3));
        int movingAverageScore = round(lastValuesAverage(scoreHistory, 3));

        LocalDateTime anchor = history.stream()
                .map(EventAiRankedEventDto::getStartDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        List<EventAiSchedulingTrendPointDto> forecast = new ArrayList<>();
        int previousParticipants = historicalTrend.get(historicalTrend.size() - 1).getParticipants();

        for (int i = 1; i <= 3; i++) {
            LocalDateTime projectedDate = anchor.plusMonths(i);
            int participants = Math.max(0, round(movingAverageParticipants + (slope * i)));
            int fillRate = clamp(round(movingAverageFillRate + (slope * 0.45 * i)), 25, 100);
            int score = clamp(round(movingAverageScore + (slope * 0.30 * i)), 20, 100);
            int changeRate = previousParticipants <= 0
                    ? 0
                    : round(((participants - previousParticipants) / (double) previousParticipants) * 100);

            forecast.add(EventAiSchedulingTrendPointDto.builder()
                    .label(monthShort(projectedDate.getMonthValue()) + " " + projectedDate.getYear())
                    .shortLabel(monthShort(projectedDate.getMonthValue()))
                    .participants(participants)
                    .fillRate(fillRate)
                    .score(score)
                    .projected(true)
                    .changeRate(changeRate)
                    .build());

            previousParticipants = participants;
        }

        return forecast;
    }

    private String schedulingKey(LocalDateTime date, String mode) {
        return switch (mode) {
            case "day" -> String.valueOf(date.getDayOfWeek().getValue());
            case "hour" -> String.valueOf(date.getHour());
            case "month" -> String.valueOf(date.getMonthValue());
            default -> "0";
        };
    }

    private String schedulingLabel(String key, String mode) {
        int value = Integer.parseInt(key);
        return switch (mode) {
            case "day" -> switch (value) {
                case 1 -> "Lundi";
                case 2 -> "Mardi";
                case 3 -> "Mercredi";
                case 4 -> "Jeudi";
                case 5 -> "Vendredi";
                case 6 -> "Samedi";
                case 7 -> "Dimanche";
                default -> key;
            };
            case "hour" -> String.format("%02dh-%02dh", value, (value + 3) % 24);
            case "month" -> switch (value) {
                case 1 -> "Janvier";
                case 2 -> "Fevrier";
                case 3 -> "Mars";
                case 4 -> "Avril";
                case 5 -> "Mai";
                case 6 -> "Juin";
                case 7 -> "Juillet";
                case 8 -> "Aout";
                case 9 -> "Septembre";
                case 10 -> "Octobre";
                case 11 -> "Novembre";
                case 12 -> "Decembre";
                default -> key;
            };
            default -> key;
        };
    }

    private String schedulingShortLabel(String key, String mode) {
        int value = Integer.parseInt(key);
        return switch (mode) {
            case "day" -> switch (value) {
                case 1 -> "Lun";
                case 2 -> "Mar";
                case 3 -> "Mer";
                case 4 -> "Jeu";
                case 5 -> "Ven";
                case 6 -> "Sam";
                case 7 -> "Dim";
                default -> key;
            };
            case "hour" -> String.format("%02dh", value);
            case "month" -> monthShort(value);
            default -> key;
        };
    }

    private String monthShort(int month) {
        return switch (month) {
            case 1 -> "Jan";
            case 2 -> "Fev";
            case 3 -> "Mar";
            case 4 -> "Avr";
            case 5 -> "Mai";
            case 6 -> "Juin";
            case 7 -> "Juil";
            case 8 -> "Aou";
            case 9 -> "Sep";
            case 10 -> "Oct";
            case 11 -> "Nov";
            case 12 -> "Dec";
            default -> String.valueOf(month);
        };
    }

    private String buildSchedulingRecommendationTitle(EventAiSchedulingSlotDto bestDay, EventAiSchedulingSlotDto bestHour) {
        if (bestDay == null && bestHour == null) {
            return "Creneau recommande en construction";
        }
        if (bestDay == null) {
            return bestHour.getLabel();
        }
        if (bestHour == null) {
            return bestDay.getLabel();
        }
        return bestDay.getLabel() + " - " + bestHour.getLabel();
    }

    private String buildSchedulingNarrative(EventAiSchedulingSlotDto bestDay, EventAiSchedulingSlotDto bestHour) {
        if (bestDay == null || bestHour == null) {
            return "Pas assez d'historique pour isoler un creneau optimal fiable.";
        }
        return "Les evenements organises " + bestDay.getLabel().toLowerCase()
                + " autour de " + bestHour.getLabel().toLowerCase()
                + " performent le mieux dans l'historique du club.";
    }

    private int computeSchedulingConfidence(int sampleSize) {
        return confidenceFor(sampleSize);
    }

    private int confidenceFor(int sampleSize) {
        if (sampleSize >= 12) return 92;
        if (sampleSize >= 8) return 84;
        if (sampleSize >= 5) return 74;
        if (sampleSize >= 3) return 63;
        return 48;
    }

    private String computeTrendDirection(
            List<EventAiSchedulingTrendPointDto> historicalTrend,
            List<EventAiSchedulingTrendPointDto> forecastTrend
    ) {
        if (historicalTrend.isEmpty() || forecastTrend.isEmpty()) {
            return "stable";
        }

        int lastHistorical = historicalTrend.get(historicalTrend.size() - 1).getParticipants();
        int lastForecast = forecastTrend.get(forecastTrend.size() - 1).getParticipants();
        if (lastHistorical <= 0) {
            return lastForecast > 0 ? "up" : "stable";
        }

        int deltaPercent = round(((lastForecast - lastHistorical) / (double) lastHistorical) * 100);
        if (deltaPercent >= 8) return "up";
        if (deltaPercent <= -8) return "down";
        return "stable";
    }

    private int computeTrendDeltaPercent(
            List<EventAiSchedulingTrendPointDto> historicalTrend,
            List<EventAiSchedulingTrendPointDto> forecastTrend
    ) {
        if (historicalTrend.isEmpty() || forecastTrend.isEmpty()) {
            return 0;
        }

        int lastHistorical = historicalTrend.get(historicalTrend.size() - 1).getParticipants();
        int lastForecast = forecastTrend.get(forecastTrend.size() - 1).getParticipants();
        if (lastHistorical <= 0) {
            return 0;
        }
        return round(((lastForecast - lastHistorical) / (double) lastHistorical) * 100);
    }

    private int computeSchedulingSuccessProbability(
            int confidence,
            int expectedLift,
            EventAiSchedulingSlotDto bestDay,
            EventAiSchedulingSlotDto bestHour,
            String trendDirection
    ) {
        int slotScore = round((((bestDay != null ? bestDay.getScore() : 50) + (bestHour != null ? bestHour.getScore() : 50)) / 2.0));
        int trendBonus = switch (trendDirection) {
            case "up" -> 8;
            case "down" -> -8;
            default -> 0;
        };

        return clamp(round((confidence * 0.45) + (slotScore * 0.35) + (Math.min(25, expectedLift) * 0.8) + trendBonus), 35, 99);
    }

    private String buildForecastNarrative(
            String trendDirection,
            int trendDeltaPercent,
            EventAiSchedulingSlotDto bestDay,
            EventAiSchedulingSlotDto bestHour
    ) {
        String slotText = bestDay != null && bestHour != null
                ? bestDay.getLabel().toLowerCase() + " autour de " + bestHour.getLabel().toLowerCase()
                : "sur les meilleurs creneaux detectes";

        return switch (trendDirection) {
            case "up" -> "La tendance prevoit une hausse d'environ " + trendDeltaPercent + "% de participation, surtout " + slotText + ".";
            case "down" -> "La tendance anticipe un leger recul de participation. Priorisez " + slotText + " pour compenser la baisse.";
            default -> "La tendance reste globalement stable. Les meilleurs resultats devraient rester concentres " + slotText + ".";
        };
    }

    private List<String> buildForecastHighlights(
            String trendDirection,
            int trendDeltaPercent,
            EventAiSchedulingSlotDto bestDay,
            EventAiSchedulingSlotDto bestHour,
            EventAiSchedulingSlotDto bestMonth
    ) {
        List<String> highlights = new ArrayList<>();

        if ("up".equals(trendDirection)) {
            highlights.add("Croissance projetee de " + trendDeltaPercent + "% sur les prochains creneaux.");
        } else if ("down".equals(trendDirection)) {
            highlights.add("Ralentissement detecte: ajuster la communication et la date.");
        } else {
            highlights.add("Tendance stable: performances regulieres dans l'historique recent.");
        }

        if (bestDay != null) {
            highlights.add(bestDay.getLabel() + " reste le jour le plus performant.");
        }

        if (bestHour != null) {
            highlights.add(bestHour.getLabel() + " concentre le meilleur niveau de remplissage.");
        }

        if (bestMonth != null) {
            highlights.add(bestMonth.getLabel() + " est la periode saisonniere la plus favorable.");
        }

        return highlights.stream().limit(4).toList();
    }

    private double computeAverageDelta(List<Integer> values) {
        if (values.size() < 2) {
            return 0;
        }

        double total = 0;
        int count = 0;
        for (int i = 1; i < values.size(); i++) {
            total += values.get(i) - values.get(i - 1);
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    private double lastValuesAverage(List<Integer> values, int size) {
        if (values.isEmpty()) {
            return 0;
        }

        int fromIndex = Math.max(0, values.size() - size);
        return values.subList(fromIndex, values.size()).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int round(double value) {
        return (int) Math.round(value);
    }

    private int computeWeightedScore(Map<String, Integer> scores, Map<String, Double> weights) {
        if (scores.isEmpty()) return 0;

        double totalWeight = weights.values().stream()
                .filter(weight -> weight > 0)
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalWeight <= 0) {
            return round(scores.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        }

        double weighted = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            double weight = Math.max(0, weights.getOrDefault(entry.getKey(), 0d));
            if (weight == 0) continue;
            weighted += entry.getValue() * (weight / totalWeight);
        }
        return Math.max(0, Math.min(100, round(weighted)));
    }

    private String computeMomentum(int trendScore) {
        if (trendScore >= 80) return "hot";
        if (trendScore >= 55) return "rising";
        return "stable";
    }

    private String resolveOrganizerBadge(int participationScore, int trendScore, int popularityScore, Event event) {
        if (isAlmostFull(event)) return "Almost Full";
        if (trendScore >= 78) return "Trending";
        if (popularityScore >= 70 || participationScore >= 72) return "Popular";
        return "Stable";
    }

    private String resolveParticipantLabel(int score, int trendScore, int affinityScore, Event event, boolean profileColdStart) {
        if ((score >= 84 && affinityScore >= 60) || affinityScore >= 76 || (profileColdStart && trendScore >= 72)) return "Recommended";
        if (isAlmostFull(event)) return "Almost Full";
        if (trendScore >= 74) return "Trending";
        if (score >= 76) return "Popular";
        return "Explore";
    }

    private String computeUrgencyLevel(Event event, int availabilityScore) {
        if (isAlmostFull(event) || availabilityScore >= 84) return "high";
        if (event.getStartDate() == null) return "low";
        long daysUntil = Duration.between(LocalDateTime.now(), event.getStartDate()).toDays();
        if (daysUntil <= 3) return "medium";
        return "low";
    }

    private boolean isAlmostFull(Event event) {
        Integer seatsLeft = extractSeatsLeft(event);
        return seatsLeft != null && seatsLeft > 0 && seatsLeft <= 3;
    }

    private Integer extractSeatsLeft(Event event) {
        if (event.getCapacity() == null || event.getCapacity() <= 0) {
            return null;
        }
        return Math.max(0, event.getCapacity() - Optional.ofNullable(event.getParticipantsCount()).orElse(0));
    }

    private Integer normalizeAvailableSeats(Event event) {
        Integer seatsLeft = extractSeatsLeft(event);
        return seatsLeft != null ? seatsLeft : Integer.MAX_VALUE;
    }

    private boolean isArchivedStatus(String status) {
        return "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    private Map<String, Double> organizerWeightMap() {
        return Map.of(
                "Popularity", organizerPopularityWeight,
                "Trend", organizerTrendWeight,
                "Urgency", organizerUrgencyWeight,
                "Conversion", organizerConversionWeight
        );
    }

    private Map<String, Double> participantWeightMap() {
        return Map.of(
                "Popularity", participantPopularityWeight,
                "Trend", participantTrendWeight,
                "Urgency", participantUrgencyWeight,
                "Affinity", participantAffinityWeight
        );
    }

    private boolean isCacheFresh(CachedDashboard<?> cachedDashboard) {
        if (cachedDashboard == null || cachedDashboard.generatedAt() == null) return false;
        long seconds = Duration.between(cachedDashboard.generatedAt(), LocalDateTime.now()).toSeconds();
        return seconds >= 0 && seconds <= Math.max(30, cacheTtlSeconds);
    }

    private EventAiDashboardDto emptyOrganizerDashboard() {
        return EventAiDashboardDto.builder()
                .avgScore(0)
                .trending(0)
                .popular(0)
                .almostFull(0)
                .statistics(List.of())
                .rankedEvents(List.of())
                .highlights(List.of())
                .trendingEvents(List.of())
                .almostFullEvents(List.of())
                .build();
    }

    private EventAiSchedulingDto emptyOrganizerScheduling() {
        return EventAiSchedulingDto.builder()
                .recommendationTitle("Creneau recommande en construction")
                .recommendationNarrative("Pas assez d'historique pour calculer un creneau optimal.")
                .forecastNarrative("Ajoutez plus d'evenements passes pour activer la prediction de tendance.")
                .predictionParticipants(0)
                .expectedLift(0)
                .confidence(0)
                .successProbability(0)
                .trendDirection("stable")
                .trendDeltaPercent(0)
                .analyzedPastEvents(0)
                .topDays(List.of())
                .topHours(List.of())
                .historicalTrend(List.of())
                .forecastTrend(List.of())
                .monthlyTrend(List.of())
                .forecastHighlights(List.of())
                .build();
    }

    private ParticipantAiDashboardDto emptyParticipantDashboard() {
        return ParticipantAiDashboardDto.builder()
                .recommended(0)
                .trending(0)
                .urgent(0)
                .profileSignals(0)
                .profileSummary("Cold-start mode: recommendations will appear once events are available.")
                .statistics(List.of())
                .recommendations(List.of())
                .trendingEvents(List.of())
                .urgentEvents(List.of())
                .build();
    }

    private record PreferenceProfile(List<String> preferredCategories, List<Long> joinedCampaignIds, int averageParticipationVolume) {
        boolean isColdStart() {
            return preferredCategories.isEmpty() && joinedCampaignIds.isEmpty() && averageParticipationVolume <= 0;
        }
    }

    private record CachedDashboard<T>(T payload, LocalDateTime generatedAt) {
    }
}
