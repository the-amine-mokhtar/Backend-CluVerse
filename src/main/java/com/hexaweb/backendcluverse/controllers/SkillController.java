package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Skill;
import com.hexaweb.backendcluverse.services.SkillService;
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
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public List<Skill> getAll() {
        return skillService.findAll();
    }

    @GetMapping("/{id}")
    public Skill getById(@PathVariable Long id) {
        return skillService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Skill create(@RequestBody Skill skill) {
        return skillService.save(skill);
    }

    @PutMapping("/{id}")
    public Skill update(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setId(id);
        return skillService.save(skill);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        skillService.deleteById(id);
    }
}

