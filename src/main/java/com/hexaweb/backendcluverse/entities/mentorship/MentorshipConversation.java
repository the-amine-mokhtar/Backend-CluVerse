package com.hexaweb.backendcluverse.entities.mentorship;

import com.hexaweb.backendcluverse.entities.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mentorship_conversations")
public class MentorshipConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentee_id", nullable = false)
    private User mentee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private com.hexaweb.backendcluverse.entities.Club club;

    @Column(nullable = false)
    private String skillName;

    @Column(nullable = false)
    private Integer mentorLevel;

    @Column(nullable = false)
    private Integer menteeLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastMessageAt;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MentorshipMessage> messages = new ArrayList<>();

    public MentorshipConversation() {
        this.createdAt = LocalDateTime.now();
        this.lastMessageAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getMentor() { return mentor; }
    public void setMentor(User mentor) { this.mentor = mentor; }

    public User getMentee() { return mentee; }
    public void setMentee(User mentee) { this.mentee = mentee; }

    public com.hexaweb.backendcluverse.entities.Club getClub() { return club; }
    public void setClub(com.hexaweb.backendcluverse.entities.Club club) { this.club = club; }

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

    public List<MentorshipMessage> getMessages() { return messages; }
    public void setMessages(List<MentorshipMessage> messages) { this.messages = messages; }

    public void addMessage(MentorshipMessage message) {
        message.setConversation(this);
        messages.add(message);
        this.lastMessageAt = message.getSentAt();
    }
}
