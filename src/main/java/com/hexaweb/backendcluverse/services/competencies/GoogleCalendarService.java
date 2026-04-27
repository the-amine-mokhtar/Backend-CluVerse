package com.hexaweb.backendcluverse.services.competencies;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.util.DateTime;
import com.hexaweb.backendcluverse.dto.Competencies.CompetencySessionScheduleRequest;
import com.hexaweb.backendcluverse.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Slf4j
public class GoogleCalendarService {

    private static final String PRIMARY_CALENDAR = "primary";

    @Value("${google.calendar.client-id:}")
    private String googleClientId;

    @Value("${google.calendar.client-secret:}")
    private String googleClientSecret;

    @Value("${google.calendar.application-name:Cluverse Backend}")
    private String applicationName;

    @Value("${google.calendar.default-timezone:UTC}")
    private String defaultTimezone;

    public void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google refresh token is required");
        }

        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Google Calendar OAuth client is not configured");
        }

        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            new GoogleRefreshTokenRequest(
                    transport,
                    GsonFactory.getDefaultInstance(),
                    refreshToken,
                    googleClientId,
                    googleClientSecret
            ).execute();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (GeneralSecurityException | java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid Google refresh token: " + ex.getMessage());
        }
    }

    public String createMeetEvent(User coach,
                                  CompetencySessionScheduleRequest request,
                                  String competencyName) {
        if (coach == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coach is required for Google Calendar event");
        }

        if (coach.getGoogleCalendarRefreshToken() == null || coach.getGoogleCalendarRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Coach Google Calendar is not connected (missing refresh token)");
        }

        if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Google Calendar OAuth client is not configured");
        }

        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

            GoogleTokenResponse tokenResponse = new GoogleRefreshTokenRequest(
                    transport,
                    GsonFactory.getDefaultInstance(),
                    coach.getGoogleCalendarRefreshToken(),
                    googleClientId,
                    googleClientSecret
            ).execute();

            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(transport)
                    .setJsonFactory(GsonFactory.getDefaultInstance())
                    .setClientSecrets(googleClientId, googleClientSecret)
                    .build();
            credential.setAccessToken(tokenResponse.getAccessToken());
            credential.setRefreshToken(coach.getGoogleCalendarRefreshToken());

            Calendar calendarClient = new Calendar.Builder(
                    transport,
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName(applicationName).build();

            String timezone = defaultTimezone == null || defaultTimezone.isBlank() ? "UTC" : defaultTimezone;

            Event event = new Event();
            event.setSummary(request.getTitle());
            event.setDescription("Competency session for: " + competencyName);

            DateTime startDateTime = new DateTime(request.getStartsAt().atZone(ZoneId.of(timezone)).toInstant().toEpochMilli());
            DateTime endDateTime = new DateTime(request.getStartsAt().plusHours(1).atZone(ZoneId.of(timezone)).toInstant().toEpochMilli());

            event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone(timezone));
            event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone(timezone));

            ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey();
            conferenceSolutionKey.setType("hangoutsMeet");

            CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest();
            createConferenceRequest.setConferenceSolutionKey(conferenceSolutionKey);
            createConferenceRequest.setRequestId(UUID.randomUUID().toString());

            ConferenceData conferenceData = new ConferenceData();
            conferenceData.setCreateRequest(createConferenceRequest);
            event.setConferenceData(conferenceData);

            Event createdEvent = calendarClient.events()
                    .insert(PRIMARY_CALENDAR, event)
                    .setConferenceDataVersion(1)
                    .execute();

            String hangoutLink = createdEvent.getHangoutLink();
            if (hangoutLink == null || hangoutLink.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Google Calendar event created without Meet link");
            }

            return hangoutLink;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (GeneralSecurityException | java.io.IOException ex) {
            log.error("Failed to create Google Calendar event for coachId={}", coach.getId(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to create Google Calendar event: " + ex.getMessage());
        }
    }
}
