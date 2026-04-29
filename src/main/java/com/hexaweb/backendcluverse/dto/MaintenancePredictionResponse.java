package com.hexaweb.backendcluverse.dto;

import java.util.List;

public class MaintenancePredictionResponse {
    private Long vehicleId;
    private String vehicleModel;
    private String vehiclePlate;
    private double breakdownRisk;        // 0-100
    private String riskLevel;            // GOOD/WARNING/CRITICAL
    private String overallAdvice;        // main recommendation
    private List<String> specificAdvices;// list of specific tips
    private int estimatedKmBeforeService;// km left before needed
    private String urgency;              // "Immédiat"/"Cette semaine"/etc
    private String modelType;
    private double predictedWeeklyDistanceKm;
    private int predictedWeeklyTransports;
    private double usagePressureScore;
    private double fleetUsageRatio;
    private int estimatedDaysUntilFailure;
    private int predictedMonthlyTransports;
    private double avgKmPerTransport;

    public MaintenancePredictionResponse() {}

    public MaintenancePredictionResponse(Long vehicleId, String vehicleModel, String vehiclePlate,
            double breakdownRisk, String riskLevel, String overallAdvice,
            List<String> specificAdvices, int estimatedKmBeforeService,
            String urgency, String modelType,
            double predictedWeeklyDistanceKm, int predictedWeeklyTransports,
            double usagePressureScore, double fleetUsageRatio) {
        this.vehicleId = vehicleId;
        this.vehicleModel = vehicleModel;
        this.vehiclePlate = vehiclePlate;
        this.breakdownRisk = breakdownRisk;
        this.riskLevel = riskLevel;
        this.overallAdvice = overallAdvice;
        this.specificAdvices = specificAdvices;
        this.estimatedKmBeforeService = estimatedKmBeforeService;
        this.urgency = urgency;
        this.modelType = modelType;
        this.predictedWeeklyDistanceKm = predictedWeeklyDistanceKm;
        this.predictedWeeklyTransports = predictedWeeklyTransports;
        this.usagePressureScore = usagePressureScore;
        this.fleetUsageRatio = fleetUsageRatio;
    }

    // Getters and Setters
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public double getBreakdownRisk() { return breakdownRisk; }
    public void setBreakdownRisk(double breakdownRisk) { this.breakdownRisk = breakdownRisk; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getOverallAdvice() { return overallAdvice; }
    public void setOverallAdvice(String overallAdvice) { this.overallAdvice = overallAdvice; }

    public List<String> getSpecificAdvices() { return specificAdvices; }
    public void setSpecificAdvices(List<String> specificAdvices) { this.specificAdvices = specificAdvices; }

    public int getEstimatedKmBeforeService() { return estimatedKmBeforeService; }
    public void setEstimatedKmBeforeService(int estimatedKmBeforeService) { this.estimatedKmBeforeService = estimatedKmBeforeService; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }

    public double getPredictedWeeklyDistanceKm() { return predictedWeeklyDistanceKm; }
    public void setPredictedWeeklyDistanceKm(double predictedWeeklyDistanceKm) { this.predictedWeeklyDistanceKm = predictedWeeklyDistanceKm; }

    public int getPredictedWeeklyTransports() { return predictedWeeklyTransports; }
    public void setPredictedWeeklyTransports(int predictedWeeklyTransports) { this.predictedWeeklyTransports = predictedWeeklyTransports; }

    public double getUsagePressureScore() { return usagePressureScore; }
    public void setUsagePressureScore(double usagePressureScore) { this.usagePressureScore = usagePressureScore; }

    public double getFleetUsageRatio() { return fleetUsageRatio; }
    public void setFleetUsageRatio(double fleetUsageRatio) { this.fleetUsageRatio = fleetUsageRatio; }

    public int getEstimatedDaysUntilFailure() { return estimatedDaysUntilFailure; }
    public void setEstimatedDaysUntilFailure(int estimatedDaysUntilFailure) { this.estimatedDaysUntilFailure = estimatedDaysUntilFailure; }

    public int getPredictedMonthlyTransports() { return predictedMonthlyTransports; }
    public void setPredictedMonthlyTransports(int predictedMonthlyTransports) { this.predictedMonthlyTransports = predictedMonthlyTransports; }

    public double getAvgKmPerTransport() { return avgKmPerTransport; }
    public void setAvgKmPerTransport(double avgKmPerTransport) { this.avgKmPerTransport = avgKmPerTransport; }
}
