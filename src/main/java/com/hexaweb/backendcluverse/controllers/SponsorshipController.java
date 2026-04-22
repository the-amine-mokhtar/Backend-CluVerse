package com.hexaweb.backendcluverse.controllers;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hexaweb.backendcluverse.dto.CompleteSponsorPaymentRequest;
import com.hexaweb.backendcluverse.dto.CreateSponsorshipRequest;
import com.hexaweb.backendcluverse.dto.MoveSponsorshipRequest;
import com.hexaweb.backendcluverse.dto.SponsorPaymentPageContextDto;
import com.hexaweb.backendcluverse.dto.SponsorshipDto;
import com.hexaweb.backendcluverse.dto.UpdateSponsorshipRequest;
import com.hexaweb.backendcluverse.services.SponsorshipService;
import com.hexaweb.backendcluverse.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sponsorships")
@RequiredArgsConstructor
public class SponsorshipController {

    private final SponsorshipService sponsorshipService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<SponsorshipDto> getAll(@RequestHeader("Authorization") String authHeader) {
        return sponsorshipService.getAll(resolveClubId(authHeader));
    }

    @GetMapping("/{id}")
    public SponsorshipDto getById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        try {
            return sponsorshipService.getById(clubId, id);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<SponsorshipDto> create(@RequestBody CreateSponsorshipRequest request,
                                                 @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        try {
            return ResponseEntity.ok(sponsorshipService.create(clubId, request));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SponsorshipDto> update(@PathVariable Long id,
                                                 @RequestBody UpdateSponsorshipRequest request,
                                                 @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        try {
            return ResponseEntity.ok(sponsorshipService.update(clubId, id, request));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<SponsorshipDto> move(@PathVariable Long id,
                                               @RequestBody MoveSponsorshipRequest request,
                                               @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        try {
            return ResponseEntity.ok(sponsorshipService.move(clubId, id, request.getToStatus()));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/sync-replies")
    public ResponseEntity<Void> syncReplies(@RequestHeader("Authorization") String authHeader) {
        sponsorshipService.triggerReplySyncAsync(resolveClubId(authHeader));
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        Long clubId = resolveClubId(authHeader);
        sponsorshipService.delete(clubId, id);
    }

    @GetMapping("/respond/accept")
    public RedirectView accept(@RequestParam("token") String token) {
        String redirectUrl = sponsorshipService.acceptOutreachByToken(token);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(redirectUrl);
        return redirectView;
    }

    @GetMapping("/respond/decline")
    public RedirectView decline(@RequestParam("token") String token) {
        String redirectUrl = sponsorshipService.declineOutreachByToken(token);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(redirectUrl);
        return redirectView;
    }

    @GetMapping(value = "/respond/upload-signed", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> uploadSignedForm(@RequestParam("token") String token) {
        return ResponseEntity.ok(sponsorshipService.getSignedUploadFormHtml(token));
    }

    @PostMapping(value = "/respond/upload-signed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RedirectView uploadSigned(@RequestParam("token") String token,
                                     @RequestParam("file") MultipartFile file) {
        RedirectView redirectView = new RedirectView();
        try {
            redirectView.setUrl(sponsorshipService.uploadSignedContractByToken(token, file));
        } catch (RuntimeException ex) {
            redirectView.setUrl(sponsorshipService.signedUploadFailureRedirect());
        }
        return redirectView;
    }

    @GetMapping("/respond/payment-context")
    public ResponseEntity<SponsorPaymentPageContextDto> paymentContext(@RequestParam("token") String token) {
        try {
            return ResponseEntity.ok(sponsorshipService.getPaymentPageContextByToken(token));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/respond/payment")
    public ResponseEntity<Void> completePayment(@RequestParam("token") String token,
                                                @RequestBody CompleteSponsorPaymentRequest request) {
        try {
            sponsorshipService.completePaymentByToken(token, request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
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

