package com.hexaweb.backendcluverse.dto.mentorship;

import java.time.LocalDateTime;

public class MentorshipFeedbackDto {
    private Long id;
    private Long conversationId;
    private Long giverId;
    private String giverName;
    private Long receiverId;
    private String receiverName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getGiverId() { return giverId; }
    public void setGiverId(Long giverId) { this.giverId = giverId; }

    public String getGiverName() { return giverName; }
    public void setGiverName(String giverName) { this.giverName = giverName; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
