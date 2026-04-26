package com.hexaweb.backendcluverse.entities.skills;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import lombok.EqualsAndHashCode;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserSkillId implements Serializable {
    private Long userId;
    private Long skillId;
}

