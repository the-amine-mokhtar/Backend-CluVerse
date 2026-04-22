package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.enumerations.CampaignStatus;
import com.hexaweb.backendcluverse.enumerations.CampaignVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Integer views = 0;
    @ManyToOne
    private Club ownerClub;

    @Enumerated(EnumType.STRING)
    private CampaignVisibility visibility;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String imageUrl;
    private String targetAudience;
    private Integer maxParticipants;

    private Integer viewsCount = 0;
    private Boolean featured = false;

    // ========= EVENTS =========
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Event> events = new ArrayList<>();

    // ========= ACCESS RIGHTS =========
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<CampaignAccess> campaignAccesses = new ArrayList<>();

    // ========= VIEWS (FIX: renamed to avoid conflict) =========
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CampaignView> campaignViews = new ArrayList<>();

    // ========= TIMESTAMPS =========
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ========= TRANSIENT FIELDS =========
    @Transient
    private Integer currentParticipants = 0;

    @Transient
    private boolean canAddEvent = false;

    // ========= DERIVED =========
    public List<Club> getSharedClubs() {
        if (campaignAccesses == null) return new ArrayList<>();
        return campaignAccesses.stream()
                .map(CampaignAccess::getClub)
                .collect(Collectors.toList());
    }

    // ========= GETTER / SETTER =========
    public boolean isCanAddEvent() {
        return canAddEvent;
    }

    public void setCanAddEvent(boolean canAddEvent) {
        this.canAddEvent = canAddEvent;
    }
}