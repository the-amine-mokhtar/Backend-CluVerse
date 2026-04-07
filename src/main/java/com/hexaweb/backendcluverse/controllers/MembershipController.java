package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Membership;
import com.hexaweb.backendcluverse.services.MembershipService;
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
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping
    public List<Membership> getAll() {
        return membershipService.findAll();
    }

    @GetMapping("/{id}")
    public Membership getById(@PathVariable Long id) {
        return membershipService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Membership create(@RequestBody Membership membership) {
        return membershipService.save(membership);
    }

    @PutMapping("/{id}")
    public Membership update(@PathVariable Long id, @RequestBody Membership membership) {
        membership.setId(id);
        return membershipService.save(membership);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        membershipService.deleteById(id);
    }
}

