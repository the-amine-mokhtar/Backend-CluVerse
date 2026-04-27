package com.hexaweb.backendcluverse.dto.mentorship;

import java.time.LocalDateTime;

public class MentorshipSessionRequestDto {
    private Long id;
    private Long conversationId;
    private Long requesterId;
    private String requesterName;
    private Long requestedUserId;
    private String requestedUserName;
    private LocalDateTime proposedDateTime;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private String responseMessage;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public Long getRequestedUserId() { return requestedUserId; }
    public void setRequestedUserId(Long requestedUserId) { this.requestedUserId = requestedUserId; }

    public String getRequestedUserName() { return requestedUserName; }
    public void setRequestedUserName(String requestedUserName) { this.requestedUserName = requestedUserName; }

    public LocalDateTime getProposedDateTime() { return proposedDateTime; }
    public void setProposedDateTime(LocalDateTime proposedDateTime) { this.proposedDateTime = proposedDateTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
}
