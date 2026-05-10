package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.enumerations.CampaignStatus;
import com.hexaweb.backendcluverse.enumerations.CampaignVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private Double budget = 0.0;

    // ✅ Boolean (wrapper) → Lombok génère getFeatured() et non isFeatured()
    private Boolean featured = false;

    // ✅ Persisté en base
    @Column(name = "events_count")
    private Integer eventsCount = 0;

    // ✅ Persisté en base
    @Column(name = "total_participants")
    private Integer totalParticipants = 0;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CampaignAccess> campaignAccesses = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CampaignView> campaignViews = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ✅ Integer (wrapper) → Lombok génère getCurrentParticipants() correctement
    @Transient
    private Integer currentParticipants = 0;

    // ✅ Boolean (wrapper) → Lombok génère getCanAddEvent() et non isCanAddEvent()
    @Transient
    private Boolean canAddEvent = false;
}