package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.ResourceRequest;
import com.hexaweb.backendcluverse.entities.Club;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.exceptions.ResourceNotFoundException;
import com.hexaweb.backendcluverse.repositories.ClubRepository;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Service
public class ResourceService extends EntityServiceImpl<Resource, Long> {
    
    private final ClubRepository clubRepository;
    private final ResourceRepository resourceRepository;
    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    public ResourceService(ResourceRepository repository, ClubRepository clubRepository) {
        super(repository);
        this.clubRepository = clubRepository;
        this.resourceRepository = repository;
    }

    public Resource createResource(ResourceRequest req) {
        Club club = clubRepository.findById(req.getClubId())
            .orElseThrow(() -> new RuntimeException("Club not found"));
        Resource r = new Resource();
        mapRequestToEntity(req, r);
        r.setClub(club);
        // Générer automatiquement un code-barres unique
        r.setBarcode(generateUniqueBarcode());
        return save(r);
    }

    public Resource updateResource(Long id, ResourceRequest req) {
        Resource r = findById(id)
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        Club club = clubRepository.findById(req.getClubId())
            .orElseThrow(() -> new RuntimeException("Club not found"));
        mapRequestToEntity(req, r);
        r.setClub(club);
        return save(r);
    }

    private void mapRequestToEntity(ResourceRequest req, Resource r) {
        r.setName(req.getName());
        r.setDescription(req.getDescription());
        r.setUnitCost(req.getUnitCost());
        r.setStatus(req.getStatus());
        r.setImageUrl(req.getImageUrl());
        r.setQuantityTotal(req.getQuantityTotal());
        r.setAvailableQuantity(req.getAvailableQuantity());
        r.setLastUpdated(req.getLastUpdated() != null ? req.getLastUpdated() : LocalDateTime.now());
        r.setLowStockThreshold(req.getLowStockThreshold());
        r.setNotes(req.getNotes());
    }

    /**
     * Génère un code-barres unique basé sur UUID
     */
    private String generateUniqueBarcode() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    /**
     * Cherche une ressource par son code-barres
     * Gère les espaces, normalise le code-barres et log les détails pour le debugging
     */
    public Resource findByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            logger.warn("❌ [findByBarcode] Barcode vide ou null reçu");
            throw new ResourceNotFoundException("Le code-barres ne peut pas être vide");
        }
        
        String normalizedBarcode = barcode.trim();
        logger.info("🔍 [findByBarcode] Recherche barcode: '{}' (length={}, bytes={})", 
            normalizedBarcode, 
            normalizedBarcode.length(),
            byteArrayToHex(normalizedBarcode.getBytes())
        );
        
        var result = resourceRepository.findByBarcode(normalizedBarcode);
        
        if (result.isPresent()) {
            logger.info("✅ [findByBarcode] Ressource trouvée: id={}, name={}, barcode={}", 
                result.get().getId(), 
                result.get().getName(),
                result.get().getBarcode()
            );
            return result.get();
        }
        
        // Logging avancé pour debugging
        logger.error("❌ [findByBarcode] Aucune ressource trouvée avec barcode: '{}'", normalizedBarcode);
        logger.error("   Barcode length: {}, Barcode bytes (hex): {}", 
            normalizedBarcode.length(),
            byteArrayToHex(normalizedBarcode.getBytes())
        );
        
        throw new ResourceNotFoundException(
            "Ressource avec le code-barres '" + normalizedBarcode + "' non trouvée. Vérifiez que le code-barres existe en base de données."
        );
    }
    
    /**
     * Utility pour convertir un byte array en hexadécimal pour le debugging
     */
    private String byteArrayToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString();
    }

    /**
     * Retourne tous les codes-barres disponibles (pour debugging)
     */
    public List<Map<String, Object>> getAllBarcodes() {
        var allResources = findAll();
        var barcodes = new ArrayList<Map<String, Object>>();
        
        for (Resource r : allResources) {
            var map = new HashMap<String, Object>();
            map.put("id", r.getId());
            map.put("name", r.getName());
            map.put("barcode", r.getBarcode());
            map.put("barcode_length", r.getBarcode() != null ? r.getBarcode().length() : 0);
            map.put("barcode_hex", r.getBarcode() != null ? byteArrayToHex(r.getBarcode().getBytes()) : "null");
            map.put("club_id", r.getClub() != null ? r.getClub().getId() : null);
            barcodes.add(map);
        }
        
        logger.info("📊 [getAllBarcodes] Total resources: {}", barcodes.size());
        barcodes.forEach(b -> logger.info("   - {}", b));
        
        return barcodes;
    }
}
