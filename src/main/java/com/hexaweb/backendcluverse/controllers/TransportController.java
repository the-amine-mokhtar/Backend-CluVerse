package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.TransportRequest;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.services.TransportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/transports")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @GetMapping
    public List<Transport> getAll() {
        return transportService.findAll();
    }

    @GetMapping("/{id}")
    public Transport getById(@PathVariable Long id) {
        return transportService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Transport create(@RequestBody TransportRequest request) {
        return transportService.createTransport(request);
    }

    @PutMapping("/{id}")
    public Transport update(@PathVariable Long id, @RequestBody TransportRequest request) {
        return transportService.updateTransport(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        transportService.deleteById(id);
    }
}
