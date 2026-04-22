package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.skills.UserSkill;
import com.hexaweb.backendcluverse.entities.skills.UserSkillId;
import com.hexaweb.backendcluverse.services.UserSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/user-skills")
@RequiredArgsConstructor
public class UserSkillController {

    private final UserSkillService userSkillService;

    @GetMapping
    public List<UserSkill> getAll() {
        return userSkillService.findAll();
    }

    @GetMapping("/{userId}/{skillId}")
    public UserSkill getById(@PathVariable Long userId, @PathVariable Long skillId) {
        UserSkillId id = new UserSkillId(userId, skillId);
        return userSkillService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public UserSkill create(@RequestBody UserSkill userSkill) {
        return userSkillService.save(userSkill);
    }

    @PutMapping("/{userId}/{skillId}")
    public UserSkill update(@PathVariable Long userId, @PathVariable Long skillId, @RequestBody UserSkill userSkill) {
        userSkill.setId(new UserSkillId(userId, skillId));
        return userSkillService.save(userSkill);
    }

    @DeleteMapping("/{userId}/{skillId}")
    public void delete(@PathVariable Long userId, @PathVariable Long skillId) {
        userSkillService.deleteById(new UserSkillId(userId, skillId));
    }
}

