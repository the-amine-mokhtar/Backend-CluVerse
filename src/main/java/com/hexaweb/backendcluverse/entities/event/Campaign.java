package com.hexaweb.backendcluverse.entities.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private double budget;

    // Une campagne peut avoir plusieurs events
    @OneToMany(mappedBy = "campaign", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();


}