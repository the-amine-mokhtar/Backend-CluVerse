package com.hexaweb.backendcluverse.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Calcul de consommation de carburant selon le modèle de véhicule
 * Basé sur des données réalistes de consommation moyenne (litres / 100km)
 */
public class FuelConsumption {
    
    private static final Map<String, Double> CONSUMPTION_RATES = new HashMap<>();
    private static final Map<String, Double> TANK_CAPACITY = new HashMap<>();
    
    static {
        // Consommation en litres / 100 km (données réalistes Tunisie)
        // Petites voitures
        CONSUMPTION_RATES.put("CLIO", 6.0);
        CONSUMPTION_RATES.put("PEUGEOT 208", 6.5);
        CONSUMPTION_RATES.put("FIAT PUNTO", 5.8);
        CONSUMPTION_RATES.put("RENAULT TWINGO", 5.5);
        
        // Véhicules midsize
        CONSUMPTION_RATES.put("PEUGEOT 3008", 8.0);
        CONSUMPTION_RATES.put("RENAULT SCENIC", 7.5);
        CONSUMPTION_RATES.put("HYUNDAI I30", 7.0);
        
        // SUV/4x4
        CONSUMPTION_RATES.put("BUS", 25.0); // bus de transport lourd
        CONSUMPTION_RATES.put("TRUCK", 30.0);
        CONSUMPTION_RATES.put("DACIA DUSTER", 8.5);
        CONSUMPTION_RATES.put("HYUNDAI IX25", 8.8);
        
        // Vans
        CONSUMPTION_RATES.put("VAN", 12.0);
        CONSUMPTION_RATES.put("SPRINTER", 11.0);
        
        // Default fallback
        
        // Capacités des réservoirs (litres)
        TANK_CAPACITY.put("CLIO", 50.0);
        TANK_CAPACITY.put("PEUGEOT 208", 52.0);
        TANK_CAPACITY.put("FIAT PUNTO", 45.0);
        TANK_CAPACITY.put("RENAULT TWINGO", 40.0);
        
        TANK_CAPACITY.put("PEUGEOT 3008", 60.0);
        TANK_CAPACITY.put("RENAULT SCENIC", 60.0);
        TANK_CAPACITY.put("HYUNDAI I30", 55.0);
        
        TANK_CAPACITY.put("BUS", 200.0);
        TANK_CAPACITY.put("TRUCK", 150.0);
        TANK_CAPACITY.put("DACIA DUSTER", 65.0);
        TANK_CAPACITY.put("HYUNDAI IX25", 60.0);
        
        TANK_CAPACITY.put("VAN", 80.0);
        TANK_CAPACITY.put("SPRINTER", 85.0);
    }
    
    /**
     * Calcule la consommation en litres pour une distance donnée
     * @param model Modèle du véhicule
     * @param distanceKm Distance en km
     * @return Consommation en litres
     */
    public static double calculateConsumptionLiters(String model, Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0) {
            return 0.0;
        }
        
        double ratePerHundred = getConsumptionRate(model);
        return (ratePerHundred * distanceKm) / 100.0;
    }
    
    /**
     * Calcule la consommation en pourcentage du réservoir
     * @param model Modèle du véhicule
     * @param distanceKm Distance en km
     * @return Pourcentage du réservoir (0-100)
     */
    public static double calculateConsumptionPercent(String model, Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0) {
            return 0.0;
        }
        
        double litersConsumed = calculateConsumptionLiters(model, distanceKm);
        double tankCapacity = getTankCapacity(model);
        
        return (litersConsumed / tankCapacity) * 100.0;
    }
    
    /**
     * Obtient le taux de consommation (litres / 100km) pour un modèle
     */
    public static double getConsumptionRate(String model) {
        if (model == null) {
            return 8.0; // Default: 8 L/100km
        }
        
        String normalizedModel = model.toUpperCase();
        
        // Exact match
        if (CONSUMPTION_RATES.containsKey(normalizedModel)) {
            return CONSUMPTION_RATES.get(normalizedModel);
        }
        
        // Partial match (contains keyword)
        for (String key : CONSUMPTION_RATES.keySet()) {
            if (normalizedModel.contains(key) || key.contains(normalizedModel)) {
                return CONSUMPTION_RATES.get(key);
            }
        }
        
        // Default
        return 8.0; // L/100km par défaut
    }
    
    /**
     * Obtient la capacité du réservoir (litres) pour un modèle
     */
    public static double getTankCapacity(String model) {
        if (model == null) {
            return 60.0; // Default: 60 litres
        }
        
        String normalizedModel = model.toUpperCase();
        
        // Exact match
        if (TANK_CAPACITY.containsKey(normalizedModel)) {
            return TANK_CAPACITY.get(normalizedModel);
        }
        
        // Partial match
        for (String key : TANK_CAPACITY.keySet()) {
            if (normalizedModel.contains(key) || key.contains(normalizedModel)) {
                return TANK_CAPACITY.get(key);
            }
        }
        
        // Default
        return 60.0; // 60 litres par défaut
    }
    
    /**
     * Estime l'autonomie restante en km
     * @param fuelLevel Niveau de carburant (0-100%)
     * @param model Modèle du véhicule
     * @return Autonomie en km
     */
    public static double calculateRemainingRange(Double fuelLevel, String model) {
        if (fuelLevel == null || fuelLevel <= 0) {
            return 0;
        }
        
        double tankCapacity = getTankCapacity(model);
        double consumptionRatePer100 = getConsumptionRate(model);
        
        double litersRemaining = (fuelLevel / 100.0) * tankCapacity;
        return (litersRemaining / consumptionRatePer100) * 100.0;
    }
}
