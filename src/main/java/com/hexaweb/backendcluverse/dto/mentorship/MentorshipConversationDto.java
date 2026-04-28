package com.hexaweb.backendcluverse.dto.mentorship;

import java.time.LocalDateTime;
import java.util.List;

public class MentorshipConversationDto {
    private Long id;
    private Long mentorId;
    private String mentorName;
    private Long menteeId;
    private String menteeName;
    private Long clubId;
    private String skillName;
    private Integer mentorLevel;
    private Integer menteeLevel;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private Boolean active;
    private List<MentorshipMessageDto> messages;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMentorId() { return mentorId; }
    public void setMentorId(Long mentorId) { this.mentorId = mentorId; }

    public String getMentorName() { return mentorName; }
    public void setMentorName(String mentorName) { this.mentorName = mentorName; }

    public Long getMenteeId() { return menteeId; }
    public void setMenteeId(Long menteeId) { this.menteeId = menteeId; }

    public String getMenteeName() { return menteeName; }
    public void setMenteeName(String menteeName) { this.menteeName = menteeName; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public Integer getMentorLevel() { return mentorLevel; }
    public void setMentorLevel(Integer mentorLevel) { this.mentorLevel = mentorLevel; }

    public Integer getMenteeLevel() { return menteeLevel; }
    public void setMenteeLevel(Integer menteeLevel) { this.menteeLevel = menteeLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<MentorshipMessageDto> getMessages() { return messages; }
    public void setMessages(List<MentorshipMessageDto> messages) { this.messages = messages; }
}
