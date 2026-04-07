package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.InboundSponsorEmailRequest;
import com.hexaweb.backendcluverse.dto.SendSponsorEmailRequest;
import com.hexaweb.backendcluverse.dto.SponsorEmailAttachmentDto;
import com.hexaweb.backendcluverse.dto.SponsorEmailDto;
import com.hexaweb.backendcluverse.entities.sponsoring.SponsorEmailAttachment;
import com.hexaweb.backendcluverse.entities.sponsoring.SponsorEmail;
import com.hexaweb.backendcluverse.enumerations.SponsorEmailDirection;
import com.hexaweb.backendcluverse.enumerations.SponsorStatus;
import com.hexaweb.backendcluverse.entities.sponsoring.Sponsor;
import com.hexaweb.backendcluverse.repositories.SponsorEmailRepository;
import com.hexaweb.backendcluverse.repositories.SponsorRepository;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SponsorService extends EntityServiceImpl<Sponsor, Long> {
    private static final Pattern SPONSOR_MARKER_PATTERN = Pattern.compile("^\\s*\\[SP-(\\d+)]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10L * 1024 * 1024;

    private final SponsorRepository sponsorRepository;
    private final SponsorEmailRepository sponsorEmailRepository;
    private final JavaMailSender mailSender;
    private final CloudinaryService cloudinaryService;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @Value("${app.mail.imap.host:imap.gmail.com}")
    private String imapHost;

    @Value("${app.mail.imap.port:993}")
    private String imapPort;

    @Value("${app.sponsor-files-dir}")
    private String sponsorFilesDir;

    @Value("${app.sponsor-files-web-base:/assets/sponsorfiles}")
    private String sponsorFilesWebBase;

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    public SponsorService(
            SponsorRepository repository,
            SponsorEmailRepository sponsorEmailRepository,
            JavaMailSender mailSender,
            CloudinaryService cloudinaryService
    ) {
        super(repository);
        this.sponsorRepository = repository;
        this.sponsorEmailRepository = sponsorEmailRepository;
        this.mailSender = mailSender;
        this.cloudinaryService = cloudinaryService;
    }

    @PostConstruct
    public void ensureSponsorFilesDirectory() {
        try {
            Files.createDirectories(Paths.get(sponsorFilesDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize sponsor files directory", e);
        }
    }

    public List<SponsorEmailDto> getEmailHistory(Long sponsorId) {
        return sponsorEmailRepository.findBySponsorIdOrderBySentAtDesc(sponsorId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SponsorEmailDto getEmailById(Long sponsorId, Long emailId) {
        SponsorEmail email = sponsorEmailRepository.findByIdAndSponsorId(emailId, sponsorId)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        return toDto(email);
    }

    public SponsorEmailDto sendEmail(Long sponsorId, SendSponsorEmailRequest request) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));

        String subject = clip(withSponsorMarker(sponsorId, sanitize(request.getSubject(), "No subject")), 250);
        String body = clip(sanitize(request.getBody(), ""), 60000);

        sendSponsorCommunicationEmail(sponsor, subject, body, Collections.emptyList());

        SponsorEmail email = new SponsorEmail();
        email.setSponsor(sponsor);
        email.setSubject(subject);
        email.setBody(body);
        email.setDirection(SponsorEmailDirection.OUTBOUND);
        email.setSentAt(LocalDateTime.now());
        email.setPinned(false);
        email.setFromAddress(clip(fromAddress, 250));
        email.setToAddress(clip(sponsor.getContactEmail(), 250));

        return toDto(sponsorEmailRepository.save(email));
    }

    public SponsorEmailDto sendEmailWithAttachments(Long sponsorId, String subjectValue, String bodyValue, MultipartFile[] files) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));

        String subject = clip(withSponsorMarker(sponsorId, sanitize(subjectValue, "No subject")), 250);
        String body = clip(sanitize(bodyValue, ""), 60000);
        List<MultipartFile> validFiles = normalizeFiles(files);

        sendSponsorCommunicationEmail(sponsor, subject, body, validFiles);

        SponsorEmail email = new SponsorEmail();
        email.setSponsor(sponsor);
        email.setSubject(subject);
        email.setBody(body);
        email.setDirection(SponsorEmailDirection.OUTBOUND);
        email.setSentAt(LocalDateTime.now());
        email.setPinned(false);
        email.setFromAddress(clip(fromAddress, 250));
        email.setToAddress(clip(sponsor.getContactEmail(), 250));

        persistAttachments(email, validFiles);
        return toDto(sponsorEmailRepository.save(email));
    }

    public SponsorEmailDto replyEmail(Long sponsorId, Long emailId, SendSponsorEmailRequest request) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));

        SponsorEmail parent = sponsorEmailRepository.findByIdAndSponsorId(emailId, sponsorId)
                .orElseThrow(() -> new RuntimeException("Original email not found"));

        String subject = sanitize(request.getSubject(), "");
        if (subject.isBlank()) {
            subject = parent.getSubject().startsWith("Re:") ? parent.getSubject() : "Re: " + parent.getSubject();
        }
        subject = clip(withSponsorMarker(sponsorId, subject), 250);
        String body = clip(sanitize(request.getBody(), ""), 60000);

        sendSponsorCommunicationEmail(sponsor, subject, body, Collections.emptyList());

        SponsorEmail reply = new SponsorEmail();
        reply.setSponsor(sponsor);
        reply.setSubject(subject);
        reply.setBody(body);
        reply.setDirection(SponsorEmailDirection.REPLY);
        reply.setInReplyToId(parent.getId());
        reply.setSentAt(LocalDateTime.now());
        reply.setPinned(false);
        reply.setFromAddress(clip(fromAddress, 250));
        reply.setToAddress(clip(sponsor.getContactEmail(), 250));
        reply.setInReplyToMessageId(clip(parent.getExternalMessageId(), 250));

        return toDto(sponsorEmailRepository.save(reply));
    }

    public SponsorEmailDto replyEmailWithAttachments(Long sponsorId, Long emailId, String subjectValue, String bodyValue, MultipartFile[] files) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));

        SponsorEmail parent = sponsorEmailRepository.findByIdAndSponsorId(emailId, sponsorId)
                .orElseThrow(() -> new RuntimeException("Original email not found"));

        String subject = sanitize(subjectValue, "");
        if (subject.isBlank()) {
            subject = parent.getSubject().startsWith("Re:") ? parent.getSubject() : "Re: " + parent.getSubject();
        }
        subject = clip(withSponsorMarker(sponsorId, subject), 250);
        String body = clip(sanitize(bodyValue, ""), 60000);
        List<MultipartFile> validFiles = normalizeFiles(files);

        sendSponsorCommunicationEmail(sponsor, subject, body, validFiles);

        SponsorEmail reply = new SponsorEmail();
        reply.setSponsor(sponsor);
        reply.setSubject(subject);
        reply.setBody(body);
        reply.setDirection(SponsorEmailDirection.REPLY);
        reply.setInReplyToId(parent.getId());
        reply.setSentAt(LocalDateTime.now());
        reply.setPinned(false);
        reply.setFromAddress(clip(fromAddress, 250));
        reply.setToAddress(clip(sponsor.getContactEmail(), 250));
        reply.setInReplyToMessageId(clip(parent.getExternalMessageId(), 250));

        persistAttachments(reply, validFiles);
        return toDto(sponsorEmailRepository.save(reply));
    }

    public SponsorEmailDto ingestInboundEmail(InboundSponsorEmailRequest request) {
        String subject = sanitize(request.getSubject(), "");
        Long sponsorId = extractSponsorIdFromSubject(subject);

        if (sponsorId == null) {
            throw new RuntimeException("Missing sponsor marker in subject. Expected format: [SP-<id>]");
        }

        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found for marker SP-" + sponsorId));

        SponsorEmail email = new SponsorEmail();
        email.setSponsor(sponsor);
        email.setSubject(clip(withSponsorMarker(sponsorId, subject), 250));
        email.setBody(clip(sanitize(request.getBody(), ""), 60000));
        email.setDirection(SponsorEmailDirection.INBOUND);
        email.setSentAt(LocalDateTime.now());
        email.setPinned(false);
        email.setFromAddress(clip(sanitize(request.getFromAddress(), ""), 250));
        email.setToAddress(clip(sanitize(request.getToAddress(), ""), 250));
        email.setExternalMessageId(clip(sanitize(request.getExternalMessageId(), null), 250));
        email.setThreadId(clip(sanitize(request.getThreadId(), null), 250));
        email.setInReplyToMessageId(clip(sanitize(request.getInReplyToMessageId(), null), 250));

        return toDto(sponsorEmailRepository.save(email));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<SponsorEmailDto> syncInboundEmailsFromMailbox() {
        List<SponsorEmailDto> synced = new ArrayList<>();
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);

        try (Store store = session.getStore("imaps")) {
            store.connect(imapHost, fromAddress, mailPassword);
            try (Folder inbox = store.getFolder("INBOX")) {
                inbox.open(Folder.READ_WRITE);
                Message[] allMessages = inbox.getMessages();
                int start = Math.max(1, allMessages.length - 120);

                for (int idx = allMessages.length; idx >= start; idx--) {
                    Message message = allMessages[idx - 1];
                    try {
                        String subject = message.getSubject();
                        Long sponsorId = extractSponsorIdFromSubject(subject);
                        if (sponsorId == null) {
                            continue;
                        }

                        String messageId = firstHeader(message, "Message-ID");
                        if (messageId != null && sponsorEmailRepository.existsByExternalMessageId(messageId)) {
                            continue;
                        }

                        Sponsor sponsor = sponsorRepository.findById(sponsorId).orElse(null);
                        if (sponsor == null) {
                            continue;
                        }

                        SponsorEmail email = new SponsorEmail();
                        email.setSponsor(sponsor);
                        email.setSubject(clip(withSponsorMarker(sponsorId, sanitize(subject, "No subject")), 250));
                        email.setBody(clip(extractTextBody(message), 60000));
                        email.setDirection(SponsorEmailDirection.INBOUND);
                        email.setSentAt(message.getSentDate() == null
                                ? LocalDateTime.now()
                                : LocalDateTime.ofInstant(message.getSentDate().toInstant(), ZoneId.systemDefault()));
                        email.setPinned(false);
                        email.setFromAddress(clip(joinAddresses(message.getFrom()), 250));
                        email.setToAddress(clip(joinAddresses(message.getRecipients(Message.RecipientType.TO)), 250));
                        email.setExternalMessageId(clip(messageId, 250));
                        email.setInReplyToMessageId(clip(firstHeader(message, "In-Reply-To"), 250));
                        email.setThreadId(clip(firstHeader(message, "Thread-Index"), 250));

                        List<InboundAttachmentData> inboundAttachments = extractInboundAttachments(message);
                        persistInboundAttachments(email, inboundAttachments);

                        synced.add(toDto(sponsorEmailRepository.save(email)));
                    } catch (Exception ignored) {
                        // Ignore malformed emails and continue processing the rest.
                    }
                }
            }
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to sync inbound sponsor emails", e);
        }

        return synced;
    }

    public SponsorEmailDto pinEmail(Long sponsorId, Long emailId) {
        SponsorEmail email = sponsorEmailRepository.findByIdAndSponsorId(emailId, sponsorId)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        email.setPinned(true);
        return toDto(sponsorEmailRepository.save(email));
    }

    public SponsorEmailDto unpinEmail(Long sponsorId, Long emailId) {
        SponsorEmail email = sponsorEmailRepository.findByIdAndSponsorId(emailId, sponsorId)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        email.setPinned(false);
        return toDto(sponsorEmailRepository.save(email));
    }

    public Sponsor createPendingSponsor(Sponsor sponsor) {
        sponsor.setStatus(SponsorStatus.PENDING);
        sponsor.setConfirmationToken(UUID.randomUUID().toString());
        sponsor.setTokenExpiresAt(LocalDateTime.now().plusHours(72));
        Sponsor saved = sponsorRepository.save(sponsor);
        sendInvitationEmail(saved);
        return saved;
    }

    public Sponsor confirmByToken(String token) {
        Sponsor sponsor = sponsorRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid confirmation token"));

        if (sponsor.getStatus() != SponsorStatus.PENDING) {
            return sponsor;
        }

        if (isExpired(sponsor)) {
            sponsor.setStatus(SponsorStatus.DENIED);
            return sponsorRepository.save(sponsor);
        }

        sponsor.setStatus(SponsorStatus.CONFIRMED);
        sponsor.setJoinDate(LocalDate.now());
        sponsor.setConfirmationToken(null);
        sponsor.setTokenExpiresAt(null);
        return sponsorRepository.save(sponsor);
    }

    public Sponsor denyByToken(String token) {
        Sponsor sponsor = sponsorRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid confirmation token"));

        if (sponsor.getStatus() == SponsorStatus.PENDING) {
            sponsor.setStatus(SponsorStatus.DENIED);
            sponsor.setConfirmationToken(null);
            sponsor.setTokenExpiresAt(null);
            return sponsorRepository.save(sponsor);
        }

        return sponsor;
    }

    private boolean isExpired(Sponsor sponsor) {
        return sponsor.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(sponsor.getTokenExpiresAt());
    }

    public Sponsor uploadLogo(Long sponsorId, MultipartFile file) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        try {
            String logoUrl = cloudinaryService.uploadLogo(file);
            sponsor.setLogoUrl(logoUrl);
            return sponsorRepository.save(sponsor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload sponsor logo", e);
        }
    }

    public void deleteWithReason(Long sponsorId, String reason) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new RuntimeException("Sponsor not found"));
        sendTerminationEmail(sponsor, reason);
        sponsorRepository.deleteById(sponsorId);
    }

    private void sendInvitationEmail(Sponsor sponsor) {
                String token = sponsor.getConfirmationToken();
                String confirmLink = "http://localhost:" + serverPort + "/api/sponsors/confirm?token=" + token;
                String denyLink = "http://localhost:" + serverPort + "/api/sponsors/deny?token=" + token;

                                String html = """
                                                                <div style="margin:0;padding:0;background:#090b1d;font-family:Inter,Segoe UI,Arial,sans-serif;">
                                                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#090b1d;padding:28px 14px;">
                                                                        <tr>
                                                                            <td align="center">
                                                                                <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;width:100%%;background:#0f132b;border:1px solid #1f2a4a;border-radius:16px;overflow:hidden;">
                                                                                    <tr>
                                                                                        <td style="padding:22px 24px;background:linear-gradient(135deg,#111a3f 0%%,#0f132b 55%%,#0b1024 100%%);border-bottom:1px solid #1f2a4a;">
                                                                                            <div style="font-size:12px;letter-spacing:0.14em;text-transform:uppercase;color:#2dd4bf;font-weight:700;">Cluverse</div>
                                                                                            <h2 style="margin:10px 0 0;color:#ffffff;font-size:24px;line-height:1.25;">Sponsorship Invitation</h2>
                                                                                        </td>
                                                                                    </tr>
                                                                                    <tr>
                                                                                        <td style="padding:24px;color:#d8def7;font-size:15px;line-height:1.7;">
                                                                                            <p style="margin:0 0 12px;">Hello <strong style="color:#ffffff;">%s</strong>,</p>
                                                                                            <p style="margin:0 0 12px;">
                                                                                                We would be honored to have your support as a sponsor. Your contribution helps us
                                                                                                build impactful student activities, events, and opportunities.
                                                                                            </p>
                                                                                            <p style="margin:0 0 18px;">Please choose one of the options below:</p>

                                                                                            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:22px 0 14px;">
                                                                                                <tr>
                                                                                                    <td style="padding-right:14px;padding-bottom:10px;">
                                                                                                        <a href="%s" style="display:inline-block;background:#2dd4bf;color:#07111f;text-decoration:none;padding:12px 18px;border-radius:10px;font-size:14px;font-weight:700;">Confirm Sponsorship</a>
                                                                                                    </td>
                                                                                                    <td style="padding-left:14px;padding-bottom:10px;">
                                                                                                        <a href="%s" style="display:inline-block;background:#e05c5c;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:10px;font-size:14px;font-weight:700;">Decline</a>
                                                                                                    </td>
                                                                                                </tr>
                                                                                            </table>

                                                                                            <div style="margin-top:8px;padding:10px 12px;background:#0b1024;border:1px solid #1f2a4a;border-radius:10px;font-size:13px;color:#93a3d9;">
                                                                                                This invitation expires in <strong style="color:#c7d2fe;">72 hours</strong>.
                                                                                            </div>
                                                                                        </td>
                                                                                    </tr>
                                                                                </table>
                                                                            </td>
                                                                        </tr>
                                                                    </table>
                                                                </div>
                                                                """.formatted(sponsor.getName(), confirmLink, denyLink);

                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom(fromAddress);
                        helper.setTo(sponsor.getContactEmail());
                        helper.setSubject("Sponsorship Invitation - " + sponsor.getName());
                        helper.setText(html, true);
                        mailSender.send(message);
                } catch (MessagingException e) {
                        throw new RuntimeException("Failed to send sponsor invitation email", e);
                }
    }

        private void sendSponsorCommunicationEmail(Sponsor sponsor, String subject, String body, List<MultipartFile> attachments) {
                String htmlBody = body.replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\n", "<br/>");

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
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                                """.formatted(subject, sponsor.getName(), htmlBody);

                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom(fromAddress);
                        helper.setTo(sponsor.getContactEmail());
                        helper.setSubject(subject);
                        helper.setText(html, true);
                        for (MultipartFile attachment : attachments) {
                            String fileName = sanitize(attachment.getOriginalFilename(), "file");
                            helper.addAttachment(fileName, attachment);
                        }
                        mailSender.send(message);
                } catch (MessagingException e) {
                        throw new RuntimeException("Failed to send sponsor email", e);
                }
        }

        private SponsorEmailDto toDto(SponsorEmail email) {
                return new SponsorEmailDto(
                                email.getId(),
                                email.getSubject(),
                                email.getBody(),
                                email.getSentAt(),
                        email.getDirection(),
                        email.getInReplyToId(),
                        email.getPinned(),
                        email.getFromAddress(),
                        email.getToAddress(),
                        email.getExternalMessageId(),
                        email.getThreadId(),
                        email.getInReplyToMessageId(),
                        email.getAttachments()
                            .stream()
                            .sorted(Comparator.comparing(SponsorEmailAttachment::getId))
                            .map(att -> new SponsorEmailAttachmentDto(
                                att.getId(),
                                att.getOriginalFileName(),
                                att.getContentType(),
                                att.getSizeBytes(),
                                att.getFileUrl()
                            ))
                            .collect(Collectors.toList())
                );
        }

            private List<MultipartFile> normalizeFiles(MultipartFile[] files) {
                if (files == null || files.length == 0) {
                    return Collections.emptyList();
                }
                List<MultipartFile> valid = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    String contentType = sanitize(file.getContentType(), "application/octet-stream");
                    boolean allowed = contentType.startsWith("image/")
                            || "application/pdf".equals(contentType)
                            || "text/plain".equals(contentType);
                    if (!allowed) {
                        throw new RuntimeException("Unsupported file type: " + contentType);
                    }
                    if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
                        throw new RuntimeException("Attachment exceeds 10MB limit: " + sanitize(file.getOriginalFilename(), "file"));
                    }
                    valid.add(file);
                }
                return valid;
            }

            private void persistAttachments(SponsorEmail email, List<MultipartFile> files) {
                if (files == null || files.isEmpty()) {
                    return;
                }
                Path targetDir = Paths.get(sponsorFilesDir);
                for (MultipartFile file : files) {
                    try {
                        String originalName = sanitize(file.getOriginalFilename(), "file");
                        String extension = "";
                        int dot = originalName.lastIndexOf('.');
                        if (dot >= 0 && dot < originalName.length() - 1) {
                            extension = "." + originalName.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "");
                        }
                        String storedFileName = UUID.randomUUID() + extension;
                        Path targetFile = targetDir.resolve(storedFileName);
                        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

                        SponsorEmailAttachment attachment = new SponsorEmailAttachment();
                        attachment.setSponsorEmail(email);
                        attachment.setOriginalFileName(clip(originalName, 250));
                        attachment.setStoredFileName(clip(storedFileName, 250));
                        attachment.setContentType(clip(sanitize(file.getContentType(), "application/octet-stream"), 120));
                        attachment.setSizeBytes(file.getSize());
                        attachment.setFileUrl(clip(buildAttachmentUrl(storedFileName), 500));
                        email.getAttachments().add(attachment);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to store attachment: " + sanitize(file.getOriginalFilename(), "file"), e);
                    }
                }
            }

            private void persistInboundAttachments(SponsorEmail email, List<InboundAttachmentData> attachments) {
                if (attachments == null || attachments.isEmpty()) {
                    return;
                }
                Path targetDir = Paths.get(sponsorFilesDir);
                for (InboundAttachmentData data : attachments) {
                    try {
                        String originalName = sanitize(data.originalFileName, "attachment");
                        String extension = "";
                        int dot = originalName.lastIndexOf('.');
                        if (dot >= 0 && dot < originalName.length() - 1) {
                            extension = "." + originalName.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "");
                        }
                        String storedFileName = UUID.randomUUID() + extension;
                        Path targetFile = targetDir.resolve(storedFileName);
                        Files.write(targetFile, data.bytes);

                        SponsorEmailAttachment attachment = new SponsorEmailAttachment();
                        attachment.setSponsorEmail(email);
                        attachment.setOriginalFileName(clip(originalName, 250));
                        attachment.setStoredFileName(clip(storedFileName, 250));
                        attachment.setContentType(clip(sanitize(data.contentType, "application/octet-stream"), 120));
                        attachment.setSizeBytes((long) data.bytes.length);
                        attachment.setFileUrl(clip(buildAttachmentUrl(storedFileName), 500));
                        email.getAttachments().add(attachment);
                    } catch (IOException e) {
                        // Skip faulty attachment without failing the whole sync.
                    }
                }
            }

            private List<InboundAttachmentData> extractInboundAttachments(Part part) {
                List<InboundAttachmentData> attachments = new ArrayList<>();
                try {
                    if (part.isMimeType("multipart/*")) {
                        Multipart multipart = (Multipart) part.getContent();
                        for (int i = 0; i < multipart.getCount(); i++) {
                            BodyPart bodyPart = multipart.getBodyPart(i);
                            String disposition = bodyPart.getDisposition();
                            String filename = bodyPart.getFileName();
                            boolean attachmentPart = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                                    || (filename != null && !filename.isBlank());

                            if (attachmentPart) {
                                byte[] bytes = readBytes(bodyPart);
                                if (bytes.length == 0 || bytes.length > MAX_ATTACHMENT_SIZE_BYTES) {
                                    continue;
                                }
                                String contentType = sanitize(bodyPart.getContentType(), "application/octet-stream");
                                contentType = contentType.split(";")[0].trim().toLowerCase();
                                boolean allowed = contentType.startsWith("image/")
                                        || "application/pdf".equals(contentType)
                                        || "text/plain".equals(contentType);
                                if (!allowed) {
                                    continue;
                                }

                                InboundAttachmentData data = new InboundAttachmentData();
                                data.originalFileName = sanitize(filename, "attachment");
                                data.contentType = contentType;
                                data.bytes = bytes;
                                attachments.add(data);
                            } else {
                                attachments.addAll(extractInboundAttachments(bodyPart));
                            }
                        }
                    }
                } catch (Exception ignored) {
                    return attachments;
                }
                return attachments;
            }

            private byte[] readBytes(Part part) {
                try {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    part.getInputStream().transferTo(buffer);
                    return buffer.toByteArray();
                } catch (Exception e) {
                    return new byte[0];
                }
            }

            private String buildAttachmentUrl(String storedFileName) {
                String webBase = sponsorFilesWebBase.startsWith("/") ? sponsorFilesWebBase : "/" + sponsorFilesWebBase;
                return appBaseUrl + webBase + "/" + storedFileName;
            }

        private String withSponsorMarker(Long sponsorId, String subject) {
            String safeSubject = sanitize(subject, "No subject");
            Matcher matcher = SPONSOR_MARKER_PATTERN.matcher(safeSubject);
            if (matcher.matches()) {
                String existingId = matcher.group(1);
                String rest = sanitize(matcher.group(2), "No subject");
                if (String.valueOf(sponsorId).equals(existingId)) {
                    return "[SP-" + sponsorId + "] " + rest;
                }
            }
            return "[SP-" + sponsorId + "] " + safeSubject;
        }

        private Long extractSponsorIdFromSubject(String subject) {
            if (subject == null) {
                return null;
            }
            Matcher matcher = SPONSOR_MARKER_PATTERN.matcher(subject);
            if (!matcher.matches()) {
                return null;
            }
            return Long.valueOf(matcher.group(1));
        }

        private String sanitize(String value, String fallback) {
                if (value == null) {
                        return fallback;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? fallback : trimmed;
        }

        private String clip(String value, int maxLength) {
            if (value == null) {
                return null;
            }
            if (value.length() <= maxLength) {
                return value;
            }
            return value.substring(0, maxLength);
        }

        private String extractTextBody(Part part) {
            try {
                if (part.isMimeType("text/plain")) {
                    Object content = part.getContent();
                    return sanitize(content == null ? "" : content.toString(), "");
                }
                if (part.isMimeType("text/html")) {
                    Object content = part.getContent();
                    String html = content == null ? "" : content.toString();
                    return sanitize(html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " "), "");
                }
                if (part.isMimeType("multipart/*")) {
                    Multipart multipart = (Multipart) part.getContent();
                    for (int i = 0; i < multipart.getCount(); i++) {
                        BodyPart bodyPart = multipart.getBodyPart(i);
                        String extracted = extractTextBody(bodyPart);
                        if (!sanitize(extracted, "").isBlank()) {
                            return extracted;
                        }
                    }
                }
            } catch (Exception ignored) {
                return "";
            }
            return "";
        }

        private String joinAddresses(Address[] addresses) {
            if (addresses == null || addresses.length == 0) {
                return "";
            }
            return java.util.Arrays.stream(addresses)
                    .map(Address::toString)
                    .collect(Collectors.joining(", "));
        }

        private String firstHeader(Message message, String headerName) {
            try {
                String[] values = message.getHeader(headerName);
                if (values == null || values.length == 0) {
                    return null;
                }
                return values[0];
            } catch (MessagingException e) {
                return null;
            }
        }

        private static class InboundAttachmentData {
            private String originalFileName;
            private String contentType;
            private byte[] bytes;
        }

        private void sendTerminationEmail(Sponsor sponsor, String reason) {
                String html = """
                                <div style="margin:0;padding:0;background:#090b1d;font-family:Inter,Segoe UI,Arial,sans-serif;">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#090b1d;padding:28px 14px;">
                                        <tr>
                                            <td align="center">
                                                <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;width:100%%;background:#0f132b;border:1px solid #1f2a4a;border-radius:16px;overflow:hidden;">
                                                    <tr>
                                                        <td style="padding:22px 24px;background:linear-gradient(135deg,#111a3f 0%%,#0f132b 55%%,#0b1024 100%%);border-bottom:1px solid #1f2a4a;">
                                                            <div style="font-size:12px;letter-spacing:0.14em;text-transform:uppercase;color:#2dd4bf;font-weight:700;">Cluverse</div>
                                                            <h2 style="margin:10px 0 0;color:#ffffff;font-size:24px;line-height:1.25;">Sponsorship Closure Notice</h2>
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding:24px;color:#d8def7;font-size:15px;line-height:1.7;">
                                                            <p style="margin:0 0 12px;">Hello <strong style="color:#ffffff;">%s</strong>,</p>
                                                            <p style="margin:0 0 12px;">
                                                                We appreciate your collaboration with our club. This message confirms that our sponsorship relationship has been closed.
                                                            </p>
                                                            <div style="margin:14px 0;padding:10px 12px;background:#0b1024;border:1px solid #1f2a4a;border-radius:10px;font-size:13px;color:#93a3d9;">
                                                                Reason: <strong style="color:#c7d2fe;">%s</strong>
                                                            </div>
                                                            <p style="margin:0;">Thank you for your time and support.</p>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                                """.formatted(sponsor.getName(), reason);

                try {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                        helper.setFrom(fromAddress);
                        helper.setTo(sponsor.getContactEmail());
                        helper.setSubject("Sponsorship closure - " + sponsor.getName());
                        helper.setText(html, true);
                        mailSender.send(message);
                } catch (MessagingException e) {
                        throw new RuntimeException("Failed to send sponsor closure email", e);
                }
        }
}

