package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.CreateSponsorshipRequest;
import com.hexaweb.backendcluverse.dto.SponsorshipDto;
import com.hexaweb.backendcluverse.dto.UpdateSponsorshipRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.event.Event;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsorship;
import com.hexaweb.backendcluverse.enumerations.SponsorEmailDirection;
import com.hexaweb.backendcluverse.enumerations.SponsorshipStatus;
import com.hexaweb.backendcluverse.repositories.EventRepository;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.SponsorEmailRepository;
import com.hexaweb.backendcluverse.repositories.SponsorRepository;
import com.hexaweb.backendcluverse.repositories.SponsorshipRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class SponsorshipService extends EntityServiceImpl<Sponsorship, Long> {
    private static final String SPONSORSHIP_THREAD_PREFIX = "SPSS-";
    private static final long REPLY_SYNC_MIN_INTERVAL_MS = 30_000L;

    private final SponsorshipRepository sponsorshipRepository;
    private final SponsorRepository sponsorRepository;
    private final EventRepository eventRepository;
    private final SponsorEmailRepository sponsorEmailRepository;
    private final SponsorService sponsorService;
    private final ClubRepository clubRepository;
    private final JavaMailSender mailSender;
    private final AtomicBoolean replySyncRunning = new AtomicBoolean(false);
    private volatile long lastReplySyncStartedAt = 0L;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    @Value("${app.sponsor-files-dir}")
    private String sponsorFilesDir;

    public SponsorshipService(
            SponsorshipRepository repository,
            SponsorRepository sponsorRepository,
            EventRepository eventRepository,
            SponsorEmailRepository sponsorEmailRepository,
            SponsorService sponsorService,
                ClubRepository clubRepository,
            JavaMailSender mailSender
    ) {
        super(repository);
        this.sponsorshipRepository = repository;
        this.sponsorRepository = sponsorRepository;
        this.eventRepository = eventRepository;
        this.sponsorEmailRepository = sponsorEmailRepository;
        this.sponsorService = sponsorService;
        this.clubRepository = clubRepository;
        this.mailSender = mailSender;
    }

    public List<SponsorshipDto> getAll(Long clubId) {
        return sponsorshipRepository.findByClubIdOrderByUpdatedAtDescIdDesc(clubId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SponsorshipDto getById(Long clubId, Long id) {
        Sponsorship sponsorship = sponsorshipRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new RuntimeException("Sponsorship not found"));
        return toDto(sponsorship);
    }

    @Transactional
    public SponsorshipDto create(Long clubId, CreateSponsorshipRequest request) {
        if (request.getSponsorId() == null) {
            throw new RuntimeException("Sponsor is required");
        }

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));

        Sponsor sponsor = sponsorRepository.findByIdAndClubId(request.getSponsorId(), clubId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));

        Sponsorship sponsorship = new Sponsorship();
        sponsorship.setSponsor(sponsor);
        sponsorship.setClub(club);
        sponsorship.setEventName(trimToNull(request.getEventName()));
        sponsorship.setOwnerName(null);
        sponsorship.setExpectedAmount(nonNegativeOrNull(request.getExpectedAmount(), "Expected amount"));
        sponsorship.setAgreedAmount(null);
        sponsorship.setProposalSummary(trimToNull(request.getProposalSummary()));
        sponsorship.setNotes(trimToNull(request.getNotes()));
        sponsorship.setStatus(SponsorshipStatus.PROSPECTING);

        if (request.getEventId() != null) {
            Optional<Event> eventOpt = eventRepository.findById(request.getEventId());
            eventOpt.ifPresent(sponsorship::setEvent);
            if (sponsorship.getEventName() == null && eventOpt.isPresent()) {
                sponsorship.setEventName(eventOpt.get().getTitle());
            }
        }

        if (sponsorship.getExpectedAmount() != null) {
            sponsorship.setAmount(sponsorship.getExpectedAmount().doubleValue());
        }

        return toDto(sponsorshipRepository.save(sponsorship));
    }

    @Transactional
    public SponsorshipDto update(Long clubId, Long id, UpdateSponsorshipRequest request) {
        Sponsorship sponsorship = sponsorshipRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new RuntimeException("Sponsorship not found"));

        SponsorshipStatus status = normalizeStatus(sponsorship.getStatus());

        if (status != SponsorshipStatus.PROSPECTING) {
            throw new RuntimeException("Cards can only be edited while in Prospecting.");
        }

        sponsorship.setEventName(trimToNull(request.getEventName()));
        sponsorship.setExpectedAmount(nonNegativeOrNull(request.getExpectedAmount(), "Expected amount"));
        sponsorship.setProposalSummary(trimToNull(request.getProposalSummary()));

        sponsorship.setNotes(trimToNull(request.getNotes()));
        sponsorship.setStartDate(request.getStartDate());
        sponsorship.setEndDate(request.getEndDate());

        if (sponsorship.getExpectedAmount() != null) {
            sponsorship.setAmount(sponsorship.getExpectedAmount().doubleValue());
        }

        if (sponsorship.getStatus() == SponsorshipStatus.SIGNED) {
            if (sponsorship.getSignedAt() == null && hasText(sponsorship.getSignedDocumentName())) {
                sponsorship.setSignedAt(LocalDateTime.now());
            }
        }

        return toDto(sponsorshipRepository.save(sponsorship));
    }

    @Transactional
    public SponsorshipDto move(Long clubId, Long id, SponsorshipStatus requestedTarget) {
        Sponsorship sponsorship = sponsorshipRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new RuntimeException("Sponsorship not found"));

        SponsorshipStatus current = normalizeStatus(sponsorship.getStatus());
        SponsorshipStatus target = normalizeStatus(requestedTarget);
        if (target == null) {
            throw new RuntimeException("Target status is required");
        }

        if (current == target) {
            sponsorship.setStatus(target);
            return toDto(sponsorshipRepository.save(sponsorship));
        }

        if (!isAdjacentTransition(current, target)) {
            throw new RuntimeException("Invalid transition. Cards can only move to the next or previous column.");
        }

        if (!isForward(current, target)) {
            throw new RuntimeException("Moving cards backwards is not allowed.");
        }

        validateForwardGuard(sponsorship, current, target);
        stampForwardTransition(sponsorship, target);

        sponsorship.setStatus(target);
        if (target == SponsorshipStatus.OUTREACH_SENT) {
            sponsorship.setOutreachDecision("PENDING");
            sponsorship.setOutreachRespondedAt(null);
            sponsorship.setOutreachResponseToken(UUID.randomUUID().toString());
        }
        Sponsorship saved = sponsorshipRepository.save(sponsorship);

        if (current == SponsorshipStatus.PROSPECTING && target == SponsorshipStatus.OUTREACH_SENT) {
            Sponsor sponsor = saved.getSponsor();
            if (sponsor != null && hasText(sponsor.getContactEmail())) {
                String eventName = firstNonBlank(saved.getEventName(), "our upcoming event");
                String subject = withSponsorshipMarker(saved, "Discover Sponsorship Opportunity - " + eventName);
                String summary = hasText(saved.getProposalSummary())
                        ? saved.getProposalSummary()
                        : "We are reaching out to invite you to support " + eventName + ". We believe this partnership can create strong visibility and impact for your brand. We would love to share full details with you.";
                String token = firstNonBlank(saved.getOutreachResponseToken(), "");
                String acceptLink = "http://localhost:" + serverPort + "/api/sponsorships/respond/accept?token=" + token;
                String declineLink = "http://localhost:" + serverPort + "/api/sponsorships/respond/decline?token=" + token;

                sendOutreachEmailAsync(
                        sponsor.getContactEmail(),
                        sponsor.getName(),
                        subject,
                        summary,
                        acceptLink,
                        declineLink
                );
            }
        }

        return toDto(saved);
    }

    private void sendOutreachEmailAsync(
            String to,
            String sponsorName,
            String subject,
            String summary,
            String acceptLink,
            String declineLink
    ) {
        Thread outreachThread = new Thread(() -> {
            try {
                String body = summary + "\n\nPlease respond by clicking one of the options below.";
                sendHtmlEmailWithOptionalAttachment(
                        to,
                        sponsorName,
                        subject,
                        body,
                        null,
                        null,
                        "Accept Sponsorship",
                        acceptLink,
                        "Decline",
                        declineLink
                );
            } catch (Exception ignored) {
                // Do not rollback status transition if email delivery fails.
            }
        }, "sponsorship-outreach-email");
        outreachThread.setDaemon(true);
        outreachThread.start();
    }

    public List<SponsorshipDto> syncReplyDrivenTransitions(Long clubId) {
        // Deprecated by button-based Accept/Decline workflow.
        return new ArrayList<>();
    }

    public void triggerReplySyncAsync(Long clubId) {
        long now = System.currentTimeMillis();
        if (now - lastReplySyncStartedAt < REPLY_SYNC_MIN_INTERVAL_MS) {
            return;
        }

        if (!replySyncRunning.compareAndSet(false, true)) {
            return;
        }

        lastReplySyncStartedAt = now;
        Thread syncThread = new Thread(() -> {
            try {
                syncReplyDrivenTransitions(clubId);
            } catch (Exception ignored) {
                // Best-effort background sync.
            } finally {
                replySyncRunning.set(false);
            }
        }, "sponsorship-reply-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    @Transactional
    public void delete(Long clubId, Long id) {
        Sponsorship sponsorship = sponsorshipRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new RuntimeException("Sponsorship not found"));
        sponsorshipRepository.delete(sponsorship);
    }

    private void validateForwardGuard(Sponsorship sponsorship, SponsorshipStatus current, SponsorshipStatus target) {
        if (current == SponsorshipStatus.OUTREACH_SENT && isDeclined(sponsorship)) {
            throw new RuntimeException("This card was declined by sponsor and can only be deleted.");
        }

        if (current == SponsorshipStatus.OUTREACH_SENT && target == SponsorshipStatus.CONTRACT_SENT) {
            throw new RuntimeException("Cannot move to Contract Sent manually. It only moves automatically when sponsor accepts from the outreach email.");
        }

        if (current == SponsorshipStatus.CONTRACT_SENT && target == SponsorshipStatus.SIGNED) {
            if (!hasText(sponsorship.getContractReference()) && !hasText(sponsorship.getSignedDocumentName())) {
                throw new RuntimeException("Cannot move to Signed: signed document or contract reference is required.");
            }
        }

        if (current == SponsorshipStatus.SIGNED && target == SponsorshipStatus.PAID) {
            BigDecimal agreed = sponsorship.getAgreedAmount();
            BigDecimal paid = nonNull(sponsorship.getPaidAmount());
            if (agreed == null || agreed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Cannot move to Paid: agreed amount must be greater than 0.");
            }
            if (paid.compareTo(agreed) < 0) {
                throw new RuntimeException("Cannot move to Paid: paid amount must be at least equal to agreed amount.");
            }
        }
    }

    private void stampForwardTransition(Sponsorship sponsorship, SponsorshipStatus target) {
        LocalDateTime now = LocalDateTime.now();
        if (target == SponsorshipStatus.OUTREACH_SENT && sponsorship.getOutreachSentAt() == null) {
            sponsorship.setOutreachSentAt(now);
        }
        if (target == SponsorshipStatus.CONTRACT_SENT && sponsorship.getContractSentAt() == null) {
            sponsorship.setContractSentAt(now);
            if (sponsorship.getProposalSentAt() == null) {
                sponsorship.setProposalSentAt(now);
            }
            if (!hasText(sponsorship.getSignedUploadToken())) {
                sponsorship.setSignedUploadToken(UUID.randomUUID().toString());
            }
        }
        if (target == SponsorshipStatus.SIGNED && sponsorship.getSignedAt() == null) {
            sponsorship.setSignedAt(now);
        }
        if (target == SponsorshipStatus.PAID && sponsorship.getPaidAt() == null) {
            sponsorship.setPaidAt(now);
        }
    }

    private boolean isForward(SponsorshipStatus current, SponsorshipStatus target) {
        return indexOf(current) < indexOf(target);
    }

    private boolean isAdjacentTransition(SponsorshipStatus current, SponsorshipStatus target) {
        return Math.abs(indexOf(current) - indexOf(target)) == 1;
    }

    private int indexOf(SponsorshipStatus status) {
        SponsorshipStatus normalized = normalizeStatus(status);
        if (normalized == SponsorshipStatus.PROSPECTING) return 0;
        if (normalized == SponsorshipStatus.OUTREACH_SENT) return 1;
        if (normalized == SponsorshipStatus.CONTRACT_SENT) return 2;
        if (normalized == SponsorshipStatus.SIGNED) return 3;
        return 4;
    }

    private SponsorshipStatus normalizeStatus(SponsorshipStatus status) {
        if (status == null) {
            return SponsorshipStatus.PROSPECTING;
        }
        if (status == SponsorshipStatus.PROPOSED || status == SponsorshipStatus.CANCELLED) {
            return SponsorshipStatus.PROSPECTING;
        }
        if (status == SponsorshipStatus.ACTIVE) {
            return SponsorshipStatus.SIGNED;
        }
        if (status == SponsorshipStatus.COMPLETED) {
            return SponsorshipStatus.PAID;
        }
        if (status == SponsorshipStatus.PROPOSAL_SENT) {
            return SponsorshipStatus.CONTRACT_SENT;
        }
        return status;
    }

    @Transactional
    public String acceptOutreachByToken(String token) {
        Sponsorship sponsorship = sponsorshipRepository.findByOutreachResponseToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid sponsorship response token"));

        if ("DECLINED".equalsIgnoreCase(sponsorship.getOutreachDecision())) {
            return appBaseUrl + "/sponsor-response?result=sponsorship-declined";
        }
        if ("ACCEPTED".equalsIgnoreCase(sponsorship.getOutreachDecision())) {
            return appBaseUrl + "/sponsor-response?result=sponsorship-accepted";
        }

        sponsorship.setOutreachDecision("ACCEPTED");
        sponsorship.setOutreachRespondedAt(LocalDateTime.now());

        if (normalizeStatus(sponsorship.getStatus()) == SponsorshipStatus.OUTREACH_SENT) {
            sponsorship.setStatus(SponsorshipStatus.CONTRACT_SENT);
            stampForwardTransition(sponsorship, SponsorshipStatus.CONTRACT_SENT);
            if (!hasText(sponsorship.getContractDocumentName())) {
                sponsorship.setContractDocumentName(defaultContractFileName(sponsorship));
            }

            Sponsorship saved = sponsorshipRepository.save(sponsorship);
            sendContractEmail(saved);
        } else {
            sponsorshipRepository.save(sponsorship);
        }

        return appBaseUrl + "/sponsor-response?result=sponsorship-accepted";
    }

    @Transactional
    public String declineOutreachByToken(String token) {
        Sponsorship sponsorship = sponsorshipRepository.findByOutreachResponseToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid sponsorship response token"));

        if ("ACCEPTED".equalsIgnoreCase(sponsorship.getOutreachDecision())) {
            return appBaseUrl + "/sponsor-response?result=sponsorship-accepted";
        }
        if ("DECLINED".equalsIgnoreCase(sponsorship.getOutreachDecision())) {
            return appBaseUrl + "/sponsor-response?result=sponsorship-declined";
        }

        sponsorship.setOutreachDecision("DECLINED");
        sponsorship.setOutreachRespondedAt(LocalDateTime.now());
        sponsorshipRepository.save(sponsorship);

        return appBaseUrl + "/sponsor-response?result=sponsorship-declined";
    }

    public String getSignedUploadFormHtml(String token) {
        boolean valid = hasText(token) && sponsorshipRepository.findBySignedUploadToken(token).isPresent();
        if (!valid) {
            return """
                    <html><body style=\"font-family:Segoe UI,Arial,sans-serif;padding:24px;background:#0b1024;color:#dbe6ff;\">
                    <h2>Invalid or expired link</h2>
                    <p>This signed contract upload link is not valid anymore.</p>
                    </body></html>
                    """;
        }

        return """
                <html>
                <body style=\"font-family:Segoe UI,Arial,sans-serif;padding:24px;background:#0b1024;color:#dbe6ff;\">
                    <div style=\"max-width:560px;margin:0 auto;background:#121a3a;border:1px solid #24325e;border-radius:12px;padding:20px;\">
                        <h2 style=\"margin-top:0;color:#ffffff;\">Upload Signed Contract</h2>
                        <p>Please upload the signed PDF contract to finalize your sponsorship agreement.</p>
                        <form method=\"post\" enctype=\"multipart/form-data\" action=\"/api/sponsorships/respond/upload-signed\">
                            <input type=\"hidden\" name=\"token\" value=\"%s\" />
                            <input type=\"file\" name=\"file\" accept=\"application/pdf,.pdf\" required style=\"margin:12px 0;display:block;\" />
                            <button type=\"submit\" style=\"background:#2dd4bf;color:#07111f;border:none;border-radius:8px;padding:10px 14px;font-weight:700;cursor:pointer;\">Submit Signed Contract</button>
                        </form>
                    </div>
                </body>
                </html>
                """.formatted(token);
    }

    @Transactional
    public String uploadSignedContractByToken(String token, MultipartFile file) {
        Sponsorship sponsorship = sponsorshipRepository.findBySignedUploadToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid signed upload token"));

        SponsorshipStatus status = normalizeStatus(sponsorship.getStatus());
        if (status == SponsorshipStatus.SIGNED || status == SponsorshipStatus.PAID) {
            return appBaseUrl + "/sponsor-response?result=signed-uploaded";
        }
        if (status != SponsorshipStatus.CONTRACT_SENT) {
            throw new RuntimeException("Signed upload is only accepted while contract is sent");
        }

        validateSignedUploadFile(file);
        String savedFileName = saveSignedPdfToDisk(sponsorship.getId(), file);

        sponsorship.setSignedDocumentName(savedFileName);
        sponsorship.setSignedAt(LocalDateTime.now());
        sponsorship.setStatus(SponsorshipStatus.SIGNED);
        sponsorship.setSignedUploadToken(null);
        sponsorshipRepository.save(sponsorship);

        return appBaseUrl + "/sponsor-response?result=signed-uploaded";
    }

    public String signedUploadFailureRedirect() {
        return appBaseUrl + "/sponsor-response?result=signed-upload-failed";
    }

    private SponsorshipDto toDto(Sponsorship sponsorship) {
        SponsorshipStatus normalizedStatus = normalizeStatus(sponsorship.getStatus());

        Sponsor sponsor = sponsorship.getSponsor();
        Event event = sponsorship.getEvent();
        BigDecimal expected = sponsorship.getExpectedAmount();

        SponsorshipDto dto = new SponsorshipDto();
        dto.setId(sponsorship.getId());
        dto.setSponsorId(sponsor != null ? sponsor.getId() : null);
        dto.setSponsorName(sponsor != null ? sponsor.getName() : null);
        dto.setSponsorLogoUrl(sponsor != null ? sponsor.getLogoUrl() : null);
        dto.setEventId(event != null ? event.getId() : null);
        dto.setEventName(firstNonBlank(sponsorship.getEventName(), event != null ? event.getTitle() : null));
        dto.setOwnerName(sponsorship.getOwnerName());
        dto.setStartDate(sponsorship.getStartDate());
        dto.setEndDate(sponsorship.getEndDate());
        dto.setAmount(BigDecimal.valueOf(sponsorship.getAmount()));
        dto.setExpectedAmount(expected);
        dto.setAgreedAmount(sponsorship.getAgreedAmount());
        dto.setPaidAmount(nonNull(sponsorship.getPaidAmount()));
        dto.setProposalSummary(sponsorship.getProposalSummary());
        dto.setProposalDocumentName(sponsorship.getProposalDocumentName());
        dto.setContractDocumentName(sponsorship.getContractDocumentName());
        dto.setSignedDocumentName(sponsorship.getSignedDocumentName());
        dto.setContractReference(sponsorship.getContractReference());
        dto.setNotes(sponsorship.getNotes());
        dto.setOutreachSentAt(sponsorship.getOutreachSentAt());
        dto.setOutreachDecision(firstNonBlank(sponsorship.getOutreachDecision(), "PENDING"));
        dto.setProposalSentAt(sponsorship.getProposalSentAt());
        dto.setContractSentAt(sponsorship.getContractSentAt());
        dto.setSignedAt(sponsorship.getSignedAt());
        dto.setPaidAt(sponsorship.getPaidAt());
        dto.setCreatedAt(sponsorship.getCreatedAt());
        dto.setUpdatedAt(sponsorship.getUpdatedAt());
        dto.setStatus(normalizedStatus);
        return dto;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private BigDecimal nonNegativeOrNull(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(label + " cannot be negative.");
        }
        return value;
    }

    private BigDecimal nonNegativeOrZero(BigDecimal value, String label) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(label + " cannot be negative.");
        }
        return value;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return second;
    }

    private boolean isDeclined(Sponsorship sponsorship) {
        return sponsorship != null && "DECLINED".equalsIgnoreCase(sponsorship.getOutreachDecision());
    }

    private void sendContractEmail(Sponsorship sponsorship) {
        Sponsor sponsor = sponsorship.getSponsor();
        if (sponsor == null || !hasText(sponsor.getContactEmail())) {
            return;
        }

        String eventName = firstNonBlank(sponsorship.getEventName(), "the event");
        String subject = withSponsorshipMarker(sponsorship, "Sponsorship Contract - " + eventName);
        String body = "Thank you for your positive response. Please find attached the sponsorship contract for " + eventName + ". We look forward to your confirmation.";
        String fileName = firstNonBlank(sponsorship.getContractDocumentName(), defaultContractFileName(sponsorship));
        if (!hasText(sponsorship.getSignedUploadToken())) {
            sponsorship.setSignedUploadToken(UUID.randomUUID().toString());
            sponsorship = sponsorshipRepository.save(sponsorship);
        }
        String signedUploadToken = firstNonBlank(sponsorship.getSignedUploadToken(), "");
        String signedUploadLink = "http://localhost:" + serverPort + "/api/sponsorships/respond/upload-signed?token=" + signedUploadToken;

        byte[] pdf = generateContractPdf(sponsorship);
        saveContractPdfToDisk(fileName, pdf);
        sendHtmlEmailWithOptionalAttachment(
            sponsor.getContactEmail(),
            sponsor.getName(),
            subject,
            body,
            fileName,
            pdf,
            "Upload Signed Contract",
            signedUploadLink,
            null,
            null
        );
    }

    private void saveContractPdfToDisk(String fileName, byte[] pdf) {
        if (!hasText(fileName) || pdf == null || pdf.length == 0) {
            return;
        }
        try {
            Path contractsDir = Paths.get(sponsorFilesDir, "Contracts");
            Files.createDirectories(contractsDir);
            Path contractPath = contractsDir.resolve(fileName);
            Files.write(contractPath, pdf);
        } catch (IOException ignored) {
            // Contract email should still be sent even if local persistence fails.
        }
    }

    private void validateSignedUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Signed contract file is required");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("Signed contract file exceeds 10MB");
        }
        String originalName = firstNonBlank(file.getOriginalFilename(), "").toLowerCase();
        String contentType = firstNonBlank(file.getContentType(), "").toLowerCase();
        boolean pdfByName = originalName.endsWith(".pdf");
        boolean pdfByType = contentType.contains("pdf");
        if (!pdfByName && !pdfByType) {
            throw new RuntimeException("Only PDF files are accepted");
        }
    }

    private String saveSignedPdfToDisk(Long sponsorshipId, MultipartFile file) {
        try {
            Path signedDir = Paths.get(sponsorFilesDir, "Signed");
            Files.createDirectories(signedDir);
            String fileName = "signed-sponsorship-" + sponsorshipId + "-" + System.currentTimeMillis() + ".pdf";
            Path signedPath = signedDir.resolve(fileName);
            Files.write(signedPath, file.getBytes());
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save signed contract", e);
        }
    }

    private void sendHtmlEmailWithOptionalAttachment(
            String to,
            String sponsorName,
            String subject,
            String body,
            String attachmentName,
            byte[] attachment,
            String primaryActionLabel,
            String primaryActionUrl,
            String secondaryActionLabel,
            String secondaryActionUrl
    ) {
        String htmlBody = body.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");

        String actionHtml = "";
        if (hasText(primaryActionLabel) && hasText(primaryActionUrl)) {
            String secondary = "";
            if (hasText(secondaryActionLabel) && hasText(secondaryActionUrl)) {
                secondary = "<a href=\"" + secondaryActionUrl + "\" style=\"display:inline-block;background:#3b1220;color:#fecdd3;text-decoration:none;padding:12px 18px;border-radius:10px;font-size:14px;font-weight:700;border:1px solid #7f1d1d;margin-left:10px;\">" + secondaryActionLabel + "</a>";
            }
            actionHtml = "<div style=\"margin-top:16px;\">" +
                    "<a href=\"" + primaryActionUrl + "\" style=\"display:inline-block;background:#2dd4bf;color:#07111f;text-decoration:none;padding:12px 18px;border-radius:10px;font-size:14px;font-weight:700;\">" + primaryActionLabel + "</a>" +
                    secondary +
                    "</div>";
        }

        String html = """
                <div style="margin:0;padding:0;background:#090b1d;font-family:Inter,Segoe UI,Arial,sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#090b1d;padding:28px 14px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;width:100%%;background:#0f132b;border:1px solid #1f2a4a;border-radius:16px;overflow:hidden;">
                                    <tr>
                                        <td style="padding:22px 24px;background:linear-gradient(135deg,#111a3f 0%%,#0f132b 55%%,#0b1024 100%%);border-bottom:1px solid #1f2a4a;">
                                            <div style="font-size:12px;letter-spacing:0.14em;text-transform:uppercase;color:#2dd4bf;font-weight:700;">Cluverse</div>
                                            <h2 style="margin:10px 0 0;color:#ffffff;font-size:22px;line-height:1.25;">%s</h2>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:24px;color:#d8def7;font-size:15px;line-height:1.7;">
                                            <p style="margin:0 0 12px;">Hello <strong style="color:#ffffff;">%s</strong>,</p>
                                            <div style="margin:0;padding:12px;background:#0b1024;border:1px solid #1f2a4a;border-radius:10px;">%s</div>
                                            %s
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </div>
                """.formatted(subject, firstNonBlank(sponsorName, "Partner"), htmlBody, actionHtml);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (attachment != null && attachment.length > 0 && hasText(attachmentName)) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachment), "application/pdf");
            }
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send sponsorship email", e);
        }
    }

    private byte[] generateContractPdf(Sponsorship sponsorship) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                float left = 56;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(left, y);
                content.showText("Sponsorship Contract");
                content.endText();

                y -= 34;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                content.newLineAtOffset(left, y);
                content.showText("Reference: " + firstNonBlank(sponsorship.getContractReference(), "CNTR-" + sponsorship.getId()));
                content.endText();

                y -= 18;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 11);
                content.newLineAtOffset(left, y);
                content.showText("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                content.endText();

                y -= 30;
                String sponsorName = sponsorship.getSponsor() != null ? firstNonBlank(sponsorship.getSponsor().getName(), "Sponsor") : "Sponsor";
                String eventName = firstNonBlank(sponsorship.getEventName(), "Upcoming Event");
                String agreedAmount = sponsorship.getAgreedAmount() == null ? "To be finalized" : sponsorship.getAgreedAmount().toPlainString();

                writeWrappedLine(content, left, y, "This contract confirms the sponsorship partnership between " + sponsorName + " and Cluverse for the event \"" + eventName + "\".");
                y -= 44;
                writeWrappedLine(content, left, y, "Agreed Sponsorship Amount: " + agreedAmount);
                y -= 28;
                writeWrappedLine(content, left, y, "Both parties acknowledge deliverables, communication, and payment commitments as discussed in writing.");
                y -= 50;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                content.newLineAtOffset(left, y);
                content.showText("Signatures");
                content.endText();

                y -= 30;
                writeWrappedLine(content, left, y, "Sponsor Representative: _________________________");
                y -= 22;
                writeWrappedLine(content, left, y, "Cluverse Representative: ________________________");
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }

    private void writeWrappedLine(PDPageContentStream content, float left, float y, String line) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 11);
        content.newLineAtOffset(left, y);
        content.showText(line);
        content.endText();
    }

    private String defaultContractFileName(Sponsorship sponsorship) {
        return "contract-sponsorship-" + sponsorship.getId() + ".pdf";
    }

    private String sponsorshipThreadMarker(Sponsorship sponsorship) {
        if (sponsorship == null || sponsorship.getId() == null) {
            return "[" + SPONSORSHIP_THREAD_PREFIX + "0]";
        }
        return "[" + SPONSORSHIP_THREAD_PREFIX + sponsorship.getId() + "]";
    }

    private String withSponsorMarker(Sponsorship sponsorship, String subject) {
        if (sponsorship == null || sponsorship.getSponsor() == null || sponsorship.getSponsor().getId() == null) {
            return subject;
        }
        String safeSubject = sanitize(subject, "No subject");
        String withoutMarkers = safeSubject.replaceAll("(?i)\\[SP-\\d+\\]", "").trim();
        return "[SP-" + sponsorship.getSponsor().getId() + "] " + sanitize(withoutMarkers, "No subject");
    }

    private String withSponsorshipMarker(Sponsorship sponsorship, String subject) {
        String sponsorTagged = withSponsorMarker(sponsorship, subject);
        String safeSubject = sanitize(sponsorTagged, "No subject");
        String withoutSponsorshipMarkers = safeSubject.replaceAll("(?i)\\[SPSS-\\d+\\]", "").trim();
        return sponsorshipThreadMarker(sponsorship) + " " + sanitize(withoutSponsorshipMarkers, "No subject");
    }

    private String sanitize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}

