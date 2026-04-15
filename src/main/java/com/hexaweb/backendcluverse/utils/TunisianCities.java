package com.hexaweb.backendcluverse.utils;

import java.util.HashMap;
import java.util.Map;

public class TunisianCities {
    
    // Real GPS coordinates of major Tunisian cities (latitude, longitude)
    public static final Map<String, double[]> COORDINATES = new HashMap<>();
    static {
        // Standard English names
        COORDINATES.put("Tunis",       new double[]{36.8189, 10.1658});
        COORDINATES.put("Sfax",        new double[]{34.7398, 10.7600});
        COORDINATES.put("Sousse",      new double[]{35.8245, 10.6346});
        COORDINATES.put("Kairouan",    new double[]{35.6781, 10.0963});
        COORDINATES.put("Bizerte",     new double[]{37.2744, 9.8739});
        COORDINATES.put("Gabes",       new double[]{33.8814, 10.0982});
        COORDINATES.put("Ariana",      new double[]{36.8625, 10.1956});
        COORDINATES.put("Gafsa",       new double[]{34.4250, 8.7842});
        COORDINATES.put("Monastir",    new double[]{35.7643, 10.8113});
        COORDINATES.put("Ben Arous",   new double[]{36.7531, 10.2281});
        COORDINATES.put("Kasserine",   new double[]{35.1676, 8.8365});
        COORDINATES.put("Medenine",    new double[]{33.3549, 10.5055});
        COORDINATES.put("Nabeul",      new double[]{36.4561, 10.7376});
        COORDINATES.put("Tataouine",   new double[]{32.9211, 10.4511});
        COORDINATES.put("Beja",        new double[]{36.7256, 9.1817});
        COORDINATES.put("Jendouba",    new double[]{36.5011, 8.7757});
        COORDINATES.put("Mahdia",      new double[]{35.5047, 11.0622});
        COORDINATES.put("Sidi Bouzid", new double[]{35.0382, 9.4849});
        COORDINATES.put("Siliana",     new double[]{36.0853, 9.3708});
        COORDINATES.put("Tozeur",      new double[]{33.9197, 8.1335});
        COORDINATES.put("Kebili",      new double[]{33.7042, 8.9694});
        COORDINATES.put("Zaghouan",    new double[]{36.4021, 10.1429});
        COORDINATES.put("Manouba",     new double[]{36.8100, 10.0972});
        COORDINATES.put("Jerba",       new double[]{33.8076, 10.8451});
        
        // Case-insensitive variants from database
        COORDINATES.put("ariana",      new double[]{36.8625, 10.1956});
        COORDINATES.put("Ariana Soghra", new double[]{36.8500, 10.2000});
        COORDINATES.put("ariana soghra", new double[]{36.8500, 10.2000});
        COORDINATES.put("bizerte",     new double[]{37.2744, 9.8739});
        COORDINATES.put("kairouen",    new double[]{35.6781, 10.0963});
        COORDINATES.put("jendouba",    new double[]{36.5011, 8.7757});
        COORDINATES.put("jerba",       new double[]{33.8076, 10.8451});
        COORDINATES.put("beja",        new double[]{36.7256, 9.1817});
        COORDINATES.put("tunis",       new double[]{36.8189, 10.1658});
        COORDINATES.put("sfax",        new double[]{34.7398, 10.7600});
        COORDINATES.put("sousse",      new double[]{35.8245, 10.6346});
    }

    /**
     * Find coordinates by city name with case-insensitive fuzzy matching
     */
    public static double[] findCoordinates(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) return null;
        
        String normalized = cityName.trim().toLowerCase();
        
        // Direct match (case-insensitive exact)
        for (Map.Entry<String, double[]> entry : COORDINATES.entrySet()) {
            if (entry.getKey().toLowerCase().equals(normalized)) {
                return entry.getValue();
            }
        }
        
        // Partial match — city name contains or is contained
        for (Map.Entry<String, double[]> entry : COORDINATES.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains(normalized) || normalized.contains(key)) {
                return entry.getValue();
            }
        }
        
        return null; // not found
    }

    /**
     * Haversine formula — calculates great-circle distance between two points on Earth
     * Returns distance in kilometers
     */
    public static double distanceKm(double lat1, double lon1, 
                                     double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) 
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    /**
     * Convert straight-line distance to estimated road distance
     * Tunisian roads factor: ~1.35 (roads are not perfectly straight due to geography)
     */
    public static double roadDistanceKm(double straightKm) {
        return straightKm * 1.35;
    }
    
    /**
     * Estimate travel duration in minutes based on road distance
     * Average speeds: 60km/h in cities, 80km/h on regional roads, 90km/h on highways
     */
    public static double estimateDurationMinutes(double roadKm) {
        if (roadKm < 30) {
            return (roadKm / 60.0) * 60;  // city speed: 60km/h
        } else if (roadKm < 100) {
            return (roadKm / 80.0) * 60;  // regional speed: 80km/h
        } else {
            return (roadKm / 90.0) * 60;  // highway speed: 90km/h
        }
    }
}
