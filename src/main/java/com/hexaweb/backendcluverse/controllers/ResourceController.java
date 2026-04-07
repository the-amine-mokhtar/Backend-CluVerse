package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.services.CloudinaryService;
import com.hexaweb.backendcluverse.services.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public List<Resource> getAll() {
        return resourceService.findAll();
    }

    @GetMapping("/{id}")
    public Resource getById(@PathVariable Long id) {
        return resourceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Resource create(@RequestBody com.hexaweb.backendcluverse.dto.ResourceRequest request) {
        return resourceService.createResource(request);
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = cloudinaryService.uploadResourceImage(file);
        return ResponseEntity.ok(imageUrl);
    }

    @PutMapping("/{id}")
    public Resource update(@PathVariable Long id, @RequestBody com.hexaweb.backendcluverse.dto.ResourceRequest request) {
        return resourceService.updateResource(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        resourceService.deleteById(id);
    }
}

