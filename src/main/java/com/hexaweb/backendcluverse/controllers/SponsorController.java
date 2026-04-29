package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.InboundSponsorEmailRequest;
import com.hexaweb.backendcluverse.dto.SendSponsorEmailRequest;
import com.hexaweb.backendcluverse.dto.SponsorEmailDto;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.services.SponsorService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/sponsors")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;
    private final JwtUtil jwtUtil;

    @Value("${app.base-url}")
    private String frontendBaseUrl;

    @GetMapping
    public List<Sponsor> getAll(@RequestHeader("Authorization") String authHeader) {
        return sponsorService.findAllByClub(resolveClubId(authHeader));
    }

    @GetMapping("/{id}")
    public Sponsor getById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        return sponsorService.findByIdAndClub(id, resolveClubId(authHeader))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Sponsor create(@RequestBody Sponsor sponsor, @RequestHeader("Authorization") String authHeader) {
        return sponsorService.createPendingSponsor(resolveClubId(authHeader), sponsor);
    }

    @PutMapping("/{id}")
    public Sponsor update(@PathVariable Long id,
                         @RequestBody Sponsor sponsor,
                         @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        sponsor.setId(id);
        sponsorService.findByIdAndClub(id, clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        sponsor.setClub(sponsorService.requireClub(clubId));
        return sponsorService.save(sponsor);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestParam String reason,
                       @RequestHeader("Authorization") String authHeader) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deletion reason is required");
        }
        sponsorService.deleteWithReason(resolveClubId(authHeader), id, reason);
    }

    @GetMapping("/{id}/emails")
    public ResponseEntity<List<SponsorEmailDto>> getEmails(@PathVariable Long id,
                                                           @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.getEmailHistory(clubId, id));
    }

    @GetMapping("/{id}/emails/{emailId}")
    public ResponseEntity<SponsorEmailDto> getEmail(@PathVariable Long id,
                                                    @PathVariable Long emailId,
                                                    @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.getEmailById(clubId, id, emailId));
    }

    @PostMapping("/{id}/emails")
    public ResponseEntity<SponsorEmailDto> sendEmail(@PathVariable Long id,
                                                      @RequestBody SendSponsorEmailRequest request,
                                                      @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.sendEmail(clubId, id, request));
    }

    @PostMapping(value = "/{id}/emails/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SponsorEmailDto> sendEmailWithFiles(@PathVariable Long id,
                                                               @RequestParam String subject,
                                                               @RequestParam(required = false) String body,
                                                               @RequestParam(required = false, name = "files") MultipartFile[] files,
                                                               @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.sendEmailWithAttachments(clubId, id, subject, body, files));
    }

    @PostMapping("/{id}/emails/{emailId}/reply")
    public ResponseEntity<SponsorEmailDto> replyEmail(@PathVariable Long id,
                                                       @PathVariable Long emailId,
                                                       @RequestBody SendSponsorEmailRequest request,
                                                       @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.replyEmail(clubId, id, emailId, request));
    }

    @PostMapping(value = "/{id}/emails/{emailId}/reply-with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SponsorEmailDto> replyEmailWithFiles(@PathVariable Long id,
                                                                @PathVariable Long emailId,
                                                                @RequestParam(required = false) String subject,
                                                                @RequestParam(required = false) String body,
                                                                @RequestParam(required = false, name = "files") MultipartFile[] files,
                                                                @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.replyEmailWithAttachments(clubId, id, emailId, subject, body, files));
    }

    @PostMapping("/{id}/emails/{emailId}/pin")
    public ResponseEntity<SponsorEmailDto> pinEmail(@PathVariable Long id,
                                                     @PathVariable Long emailId,
                                                     @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.pinEmail(clubId, id, emailId));
    }

    @PostMapping("/{id}/emails/{emailId}/unpin")
    public ResponseEntity<SponsorEmailDto> unpinEmail(@PathVariable Long id,
                                                       @PathVariable Long emailId,
                                                       @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.unpinEmail(clubId, id, emailId));
    }

    @PostMapping("/emails/inbound")
    public ResponseEntity<SponsorEmailDto> ingestInboundEmail(@RequestBody InboundSponsorEmailRequest request,
                                                              @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        return ResponseEntity.ok(sponsorService.ingestInboundEmail(clubId, request));
    }

    @PostMapping("/emails/sync-inbound")
    public ResponseEntity<List<SponsorEmailDto>> syncInboundEmails(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(sponsorService.syncInboundEmailsFromMailbox(resolveClubId(authHeader)));
    }

    @PostMapping("/{id}/logo")
    public Sponsor uploadLogo(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              @RequestHeader("Authorization") String authHeader) {
        return sponsorService.uploadLogo(resolveClubId(authHeader), id, file);
    }

    @GetMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestParam String token) {
        try {
            Sponsor sponsor = sponsorService.confirmByToken(token);
            String result = sponsor.getStatus().name().toLowerCase();
            return redirectToFrontend(result);
        } catch (RuntimeException e) {
            return redirectToFrontend("invalid");
        }
    }

    @GetMapping("/deny")
    public ResponseEntity<Void> deny(@RequestParam String token) {
        try {
            Sponsor sponsor = sponsorService.denyByToken(token);
            String result = sponsor.getStatus().name().toLowerCase();
            return redirectToFrontend(result);
        } catch (RuntimeException e) {
            return redirectToFrontend("invalid");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String result) {
        String target = frontendBaseUrl + "/sponsor-response?result=" + result;
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(target));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private Long resolveClubId(String authHeader) {
        try {
            String token = jwtUtil.resolveBearerToken(authHeader);
            Long clubId = jwtUtil.extractClubId(token);
            if (clubId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing club id in token");
            }
            return clubId;
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }
    }
}

