package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Role;
import com.hexaweb.backendcluverse.services.RoleService;
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
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<Role> getAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    public Role getById(@PathVariable Long id) {
        return roleService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Role create(@RequestBody Role role) {
        return roleService.save(role);
    }

    @PutMapping("/{id}")
    public Role update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return roleService.save(role);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        roleService.deleteById(id);
    }
}

