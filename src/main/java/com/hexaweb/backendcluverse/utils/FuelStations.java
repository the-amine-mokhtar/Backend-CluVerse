package com.hexaweb.backendcluverse.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Liste des stations-service principales en Tunisie
 * Utilisé pour afficher la première station-service à rencontrer
 */
public class FuelStations {
    
    public static class Station {
        public String name;
        public String city;
        public double latitude;
        public double longitude;
        public String services;
        
        public Station(String name, String city, double lat, double lon, String services) {
            this.name = name;
            this.city = city;
            this.latitude = lat;
            this.longitude = lon;
            this.services = services;
        }
    }
    
    private static final List<Station> STATIONS = new ArrayList<>();
    
    static {
        // Stations-services principales en Tunisie (coordonnées approximatives)
        
        // Nord (Tunis, Bizerte, Ariana)
        STATIONS.add(new Station("Elsan Tunis Centre", "Tunis", 36.8, 10.18, "Essence, Gasoil, Shop"));
        STATIONS.add(new Station("Dynacor Ariana", "Ariana", 36.87, 10.19, "Essence, Gasoil"));
        STATIONS.add(new Station("Elf Bizerte", "Bizerte", 37.27, 9.87, "Essence, Gasoil, Wash"));
        STATIONS.add(new Station("Shell Manouba", "Manouba", 36.81, 10.06, "Essence, Gasoil, Shop"));
        
        // Nord-Ouest (Béja, Jendouba)
        STATIONS.add(new Station("Elsan Béja", "Béja", 36.73, 9.18, "Essence, Gasoil"));
        STATIONS.add(new Station("Dynacor Jendouba", "Jendouba", 36.50, 8.78, "Essence, Gasoil, Wash"));
        STATIONS.add(new Station("Elf Tabarka", "Tabarka", 36.96, 8.46, "Essence, Gasoil"));
        
        // Nord-Est (Sousse, Monastir)
        STATIONS.add(new Station("Shell Sousse", "Sousse", 35.82, 10.63, "Essence, Gasoil, Shop, Wash"));
        STATIONS.add(new Station("Elsan Monastir", "Monastir", 35.77, 10.83, "Essence, Gasoil"));
        STATIONS.add(new Station("Dynacor Kairouan", "Kairouan", 35.67, 10.10, "Essence, Gasoil"));
        
        // Centre (Sfax, Gafsa)
        STATIONS.add(new Station("Elsan Sfax", "Sfax", 34.74, 10.76, "Essence, Gasoil, Shop"));
        STATIONS.add(new Station("Shell Gafsa", "Gafsa", 34.43, 8.78, "Essence, Gasoil, Wash"));
        STATIONS.add(new Station("Dinacor Sidi Bouzid", "Sidi Bouzid", 35.03, 9.49, "Essence, Gasoil"));
        
        // Sud (Gabès, Médenine)
        STATIONS.add(new Station("Elf Gabès", "Gabès", 33.88, 10.10, "Essence, Gasoil"));
        STATIONS.add(new Station("Shell Médenine", "Médenine", 33.35, 10.50, "Essence, Gasoil, Shop"));
        STATIONS.add(new Station("Dinacor Tataouine", "Tataouine", 32.93, 10.45, "Essence, Gasoil"));
        
        // Côte (Hammamet, Nabeul)
        STATIONS.add(new Station("Shell Hammamet", "Hammamet", 36.40, 10.61, "Essence, Gasoil, Shop, Wash"));
        STATIONS.add(new Station("Elsan Nabeul", "Nabeul", 36.46, 10.74, "Essence, Gasoil"));
        
        // Sud-Est (Djerba)
        STATIONS.add(new Station("Elsan Djerba", "Djerba", 33.81, 10.86, "Essence, Gasoil, Shop"));
        STATIONS.add(new Station("Shell Zarzis", "Zarzis", 33.51, 11.11, "Essence, Gasoil, Wash"));
    }
    
    /**
     * Trouve la station-service la plus proche d'une localisation
     * @param latitude Latitude de départ
     * @param longitude Longitude de départ
     * @return Station la plus proche
     */
    public static Station findNearestStation(double latitude, double longitude) {
        if (STATIONS.isEmpty()) {
            return null;
        }
        
        Station nearest = STATIONS.get(0);
        double minDistance = calculateDistance(latitude, longitude, nearest.latitude, nearest.longitude);
        
        for (Station station : STATIONS) {
            double distance = calculateDistance(latitude, longitude, station.latitude, station.longitude);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = station;
            }
        }
        
        return nearest;
    }
    
    /**
     * Trouve les N stations-services les plus proches
     */
    public static List<Station> findNearestStations(double latitude, double longitude, int limit) {
        List<Station> sorted = new ArrayList<>(STATIONS);
        
        sorted.sort((s1, s2) -> {
            double d1 = calculateDistance(latitude, longitude, s1.latitude, s1.longitude);
            double d2 = calculateDistance(latitude, longitude, s2.latitude, s2.longitude);
            return Double.compare(d1, d2);
        });
        
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }
    
    /**
     * Calcul de distance Haversine en km
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon terrestre en km
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.asin(Math.sqrt(a));
        
        return R * c;
    }
}
