package com.hexaweb.backendcluverse.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private EntityManager entityManager;

    @Transactional
    @DeleteMapping("/cleanup/transports")
    public String cleanupTransports() {
        try {
            int deleted = entityManager
                .createNativeQuery("DELETE FROM transport")
                .executeUpdate();
            
            return "✅ Supprimé " + deleted + " transports de la table";
        } catch (Exception e) {
            return "❌ Erreur: " + e.getMessage();
        }
    }

    @GetMapping("/transports/count")
    public String countTransports() {
        try {
            Long count = (Long) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM transport")
                .getSingleResult();
            
            return "Nombre de transports: " + count;
        } catch (Exception e) {
            return "Erreur: " + e.getMessage();
        }
    }
}
