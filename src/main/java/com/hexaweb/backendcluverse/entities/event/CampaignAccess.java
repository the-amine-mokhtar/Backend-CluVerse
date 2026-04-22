package com.hexaweb.backendcluverse.entities.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.enumerations.CampaignPermission;
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

    // 🔹 Campaign liée
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Campaign campaign;

    // 🔹 Club autorisé
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Club club;

    // 🔹 Permissions
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "campaign_access_permissions",
            joinColumns = @JoinColumn(name = "campaign_access_id")
    )
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    private Set<CampaignPermission> permissions = new HashSet<>();

    // =========================================================
    // HELPERS
    // =========================================================

    public boolean hasPermission(CampaignPermission permission) {
        return permissions != null && permissions.contains(permission);
    }

    public void addPermission(CampaignPermission permission) {
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        permissions.add(permission);
    }

    public void removePermission(CampaignPermission permission) {
        if (permissions != null) {
            permissions.remove(permission);
        }
    }
}