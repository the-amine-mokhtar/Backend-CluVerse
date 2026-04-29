package com.hexaweb.backendcluverse.entities.event;

import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.enumerations.CampaignPermission;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "campaign_access_permissions",
            joinColumns = @JoinColumn(name = "campaign_access_id")
    )
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<CampaignPermission> permissions = new HashSet<>();

    @Transient
    @JsonProperty("campaignId")
    public Long getCampaignId() {
        return campaign != null ? campaign.getId() : null;
    }

    @Transient
    @JsonProperty("clubId")
    public Long getClubId() {
        return club != null ? club.getId() : null;
    }

    @Transient
    @JsonProperty("clubName")
    public String getClubName() {
        return club != null ? club.getName() : null;
    }
}
