package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.Position;
import com.hexaweb.backendcluverse.services.PositionService;
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
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @GetMapping
    public List<Position> getAll() {
        return positionService.findAll();
    }

    @GetMapping("/{id}")
    public Position getById(@PathVariable Long id) {
        return positionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Position create(@RequestBody Position position) {
        return positionService.save(position);
    }

    @PutMapping("/{id}")
    public Position update(@PathVariable Long id, @RequestBody Position position) {
        position.setId(id);
        return positionService.save(position);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        positionService.deleteById(id);
    }
}

