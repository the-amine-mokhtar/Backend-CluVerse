package com.hexaweb.backendcluverse.entities.event;

import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.enumerations.WaitingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventWaitingList {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private User user;

    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WaitingStatus status = WaitingStatus.PENDING;
}