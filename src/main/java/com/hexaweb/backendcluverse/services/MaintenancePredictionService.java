package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.MaintenancePredictionResponse;
import com.hexaweb.backendcluverse.entities.logistics.Transport;
import com.hexaweb.backendcluverse.enumerations.TransportStatus;
import com.hexaweb.backendcluverse.repositories.TransportRepository;
import com.hexaweb.backendcluverse.repositories.VehicleMaintenanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaintenancePredictionService {

    @Autowired
    private MaintenanceMLModel model;
    
    @Autowired
    private VehicleMaintenanceRepository maintenanceRepo;

    @Autowired
    private TransportRepository transportRepository;

    /**
     * Calculate risk score directly without creating full response
     * Used for automatic status calculation
     */
    public double calculateRisk(int kmSinceService, int daysSinceService,
            int fuelLevel, int engineCondition,
            int tireCondition, int brakeCondition,
            int oilLevel, int totalTransports) {
        double usagePressure = Math.max(0.0, Math.min(1.0, totalTransports / 500.0));
        return model.predictRisk(
            kmSinceService, daysSinceService,
            fuelLevel, engineCondition, tireCondition,
            brakeCondition, oilLevel, usagePressure);
    }

    public MaintenancePredictionResponse predict(Long vehicleId, 
            String vehicleModel, String vehiclePlate,
            int kmSinceService, int daysSinceService,
            int fuelLevel, int engineCondition,
            int tireCondition, int brakeCondition,
            int oilLevel, int totalTransports) {

        UsageContext usage = buildUsageContext(vehicleId);

        int projectedKmSinceService = kmSinceService + (int) Math.round(usage.predictedWeeklyDistanceKm);
        int projectedDaysSinceService = Math.min(365, daysSinceService + 7);
        double projectedUsagePressure = usage.usagePressureScore;

        double risk = model.predictRisk(
            projectedKmSinceService, projectedDaysSinceService,
            fuelLevel, engineCondition, tireCondition,
            brakeCondition, oilLevel, projectedUsagePressure);

        // --- Critical Components Penalty ---
        // If oil, brakes or engine are critical, risk must be at least 40%
        if (oilLevel < 40 || brakeCondition < 40 || engineCondition < 40) {
            risk = Math.max(risk, 45.0);
        }

        // --- Safety adjustment for fresh vehicles ---
        if (usage.predictedWeeklyDistanceKm == 0 && usage.predictedWeeklyTransports == 0 && kmSinceService < 100 && oilLevel >= 40) {
            risk = Math.min(risk, 15.0); 
        }

        String riskLevel = risk < 30 ? "GOOD" : 
                           risk < 65 ? "WARNING" : "CRITICAL";

        List<String> advices = generateAdvices(
            fuelLevel, engineCondition, tireCondition,
            brakeCondition, oilLevel, 
            kmSinceService, daysSinceService);

        if (usage.fleetUsageRatio > 1.15) {
            advices.add("📈 Ce véhicule est plus sollicité que la moyenne de la flotte (x"
                + String.format("%.2f", usage.fleetUsageRatio) + ") — renforcer la maintenance préventive.");
        }

        if (usage.predictedWeeklyDistanceKm > 0) {
            advices.add("🗓 Projection 7 jours: ~"
                + (int) Math.round(usage.predictedWeeklyDistanceKm)
                + " km et " + usage.predictedWeeklyTransports
                + " transports prévus, intégrés dans le score IA.");
        }

        // --- Smart Prediction Logic (Historical based) ---
        int kmLeft = Math.max(0, (int)((100 - risk) / 100.0 * 5000));
        int daysUntilFailure = 999; // Default: very far
        if (usage.predictedWeeklyDistanceKm > 0) {
            daysUntilFailure = (int) Math.round((kmLeft / usage.predictedWeeklyDistanceKm) * 7);
        } else if (risk < 30) {
            daysUntilFailure = 365; // Good state, no usage
        } else if (risk >= 70) {
            daysUntilFailure = 3; // Critical state, even without usage
        }

        String overallAdvice = buildOverallAdvice(riskLevel, risk, daysUntilFailure, usage.predictedMonthlyTransports);
        
        String urgency;
        if (risk >= 75 || daysUntilFailure <= 7) urgency = "Intervention immédiate requise";
        else if (risk >= 50 || daysUntilFailure <= 14) urgency = "Cette semaine";
        else if (risk >= 30 || daysUntilFailure <= 30) urgency = "Ce mois";
        else urgency = "Prochain entretien régulier";

        MaintenancePredictionResponse response = 
            new MaintenancePredictionResponse();
        response.setVehicleId(vehicleId);
        response.setVehicleModel(vehicleModel);
        response.setVehiclePlate(vehiclePlate);
        response.setBreakdownRisk(Math.round(risk * 10.0) / 10.0);
        response.setRiskLevel(riskLevel);
        response.setOverallAdvice(overallAdvice);
        response.setSpecificAdvices(advices);
        response.setEstimatedKmBeforeService(kmLeft);
        response.setUrgency(urgency);
        response.setModelType(model.getModelType());
        response.setPredictedWeeklyDistanceKm(Math.round(usage.predictedWeeklyDistanceKm * 10.0) / 10.0);
        response.setPredictedWeeklyTransports(usage.predictedWeeklyTransports);
        response.setUsagePressureScore(Math.round(usage.usagePressureScore * 1000.0) / 1000.0);
        response.setFleetUsageRatio(Math.round(usage.fleetUsageRatio * 100.0) / 100.0);
        
        // New fields
        response.setEstimatedDaysUntilFailure(daysUntilFailure);
        response.setPredictedMonthlyTransports(usage.predictedMonthlyTransports);
        response.setAvgKmPerTransport(usage.avgKmPerTransport);
        
        return response;
    }

    private UsageContext buildUsageContext(Long vehicleId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(28);

        List<TransportStatus> activeStatuses = List.of(
            TransportStatus.COMPLETED, 
            TransportStatus.PLANNED, 
            TransportStatus.IN_PROGRESS
        );

        List<Transport> vehicleRecent = transportRepository
            .findByVehicle_IdAndStatusInAndScheduledDateBetween(
                vehicleId,
                activeStatuses,
                startDate,
                now.plusDays(7) // On regarde aussi 7 jours dans le futur
            );

        List<Transport> fleetRecent = transportRepository
            .findByStatusInAndScheduledDateBetween(
                activeStatuses,
                startDate,
                now.plusDays(7)
            );

        double vehicleDistance = vehicleRecent.stream()
            .mapToDouble(t -> t.getDistance() != null ? t.getDistance() : 0.0)
            .sum();
        int vehicleTrips = vehicleRecent.size();

        double fleetDistance = fleetRecent.stream()
            .mapToDouble(t -> t.getDistance() != null ? t.getDistance() : 0.0)
            .sum();
        int fleetTrips = fleetRecent.size();

        double vehicleTripsPerWeek = vehicleTrips / 4.0;
        double vehicleKmPerWeek = vehicleDistance / 4.0;

        double fleetAvgTripsPerVehiclePerWeek = vehicleTripsPerWeek;
        double fleetAvgKmPerVehiclePerWeek = vehicleKmPerWeek;

        if (!fleetRecent.isEmpty()) {
            long vehiclesInFleetWindow = fleetRecent.stream()
                .map(t -> t.getVehicle() != null ? t.getVehicle().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .count();

            if (vehiclesInFleetWindow > 0) {
                fleetAvgTripsPerVehiclePerWeek = (fleetTrips / 4.0) / vehiclesInFleetWindow;
                fleetAvgKmPerVehiclePerWeek = (fleetDistance / 4.0) / vehiclesInFleetWindow;
            }
        }

        double tripsRatio = vehicleTripsPerWeek / Math.max(0.25, fleetAvgTripsPerVehiclePerWeek);
        double distanceRatio = vehicleKmPerWeek / Math.max(5.0, fleetAvgKmPerVehiclePerWeek);
        double fleetUsageRatio = (tripsRatio + distanceRatio) / 2.0;

        // Usage pressure encoded in [0,1] for the model's usage feature.
        double usagePressureScore = Math.max(0.0, Math.min(1.0,
            0.45 * Math.min(2.5, tripsRatio) / 2.5 +
            0.45 * Math.min(2.5, distanceRatio) / 2.5 +
            0.10 * Math.min(1.0, vehicleKmPerWeek / 250.0)
        ));

        UsageContext context = new UsageContext();
        context.predictedWeeklyDistanceKm = vehicleKmPerWeek;
        context.predictedWeeklyTransports = Math.max(0, (int) Math.round(vehicleTripsPerWeek));
        context.predictedMonthlyTransports = vehicleTrips; // Real count from last 28 days
        context.avgKmPerTransport = vehicleTrips > 0 ? vehicleDistance / vehicleTrips : 0.0;
        context.usagePressureScore = usagePressureScore;
        context.fleetUsageRatio = fleetUsageRatio;
        return context;
    }

    private static class UsageContext {
        private double predictedWeeklyDistanceKm;
        private int predictedWeeklyTransports;
        private int predictedMonthlyTransports;
        private double avgKmPerTransport;
        private double usagePressureScore;
        private double fleetUsageRatio;
    }

    private List<String> generateAdvices(
            int fuel, int engine, int tire, 
            int brake, int oil,
            int kmSinceService, int daysSinceService) {
        
        List<String> advices = new ArrayList<>();

        if (fuel < 20) 
            advices.add("⛽ Niveau de carburant critique (" + fuel 
                + "%) — Faites le plein avant le prochain transport.");
        else if (fuel < 40)
            advices.add("⛽ Carburant bas (" + fuel 
                + "%) — Prévoir un plein prochainement.");

        if (oil < 20)
            advices.add("🛢 Niveau d'huile critique — "
                + "Changez l'huile immédiatement.");
        else if (oil < 40)
            advices.add("🛢 Huile moteur faible (" + oil 
                + "%) — Vérification recommandée.");

        if (engine < 40)
            advices.add("🔧 État moteur dégradé (" + engine 
                + "/100) — Consultez un mécanicien.");
        else if (engine < 60)
            advices.add("🔧 Moteur à surveiller — "
                + "Inspection recommandée lors du prochain entretien.");

        if (brake < 30)
            advices.add("🛑 Freins en état critique (" + brake 
                + "/100) — Intervention urgente requise !");
        else if (brake < 50)
            advices.add("🛑 Freins à vérifier (" + brake 
                + "/100) — Consultez un technicien cette semaine.");

        if (tire < 30)
            advices.add("🔄 Pneus très usés (" + tire 
                + "/100) — Remplacement nécessaire avant transport.");
        else if (tire < 50)
            advices.add("🔄 Pneus à surveiller — "
                + "Vérifiez la pression et l'usure.");

        if (kmSinceService > 10000)
            advices.add("📋 " + kmSinceService 
                + " km depuis le dernier entretien — "
                + "Visite technique recommandée.");
        
        if (daysSinceService > 180)
            advices.add("📅 " + daysSinceService 
                + " jours sans entretien — "
                + "Contrôle périodique nécessaire.");

        if (advices.isEmpty())
            advices.add("✅ Véhicule en bon état général. "
                + "Continuez l'entretien régulier.");

        return advices;
    }

    private String buildOverallAdvice(String riskLevel, double risk, int daysUntilFailure, int monthlyTransports) {
        String base;
        switch(riskLevel) {
            case "CRITICAL":
                base = "🚨 Risque de panne élevé (" + (int)risk + "%).";
                break;
            case "WARNING":
                base = "⚠️ Attention requise (" + (int)risk + "%).";
                break;
            default:
                base = "✅ Véhicule en bon état (" + (int)risk + "% de risque).";
                break;
        }

        if (monthlyTransports > 0) {
            String timeMsg = daysUntilFailure > 365 ? "plus d'un an" : daysUntilFailure + " jours";
            return base + " Basé sur vos " + monthlyTransports + " transports du mois dernier, "
                 + "une intervention est estimée nécessaire dans environ " + timeMsg + ".";
        } else {
            return base + " Aucune activité récente détectée. Continuez l'entretien préventif.";
        }
    }
}
