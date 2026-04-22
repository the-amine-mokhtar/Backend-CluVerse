package com.hexaweb.backendcluverse.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class MaintenanceMLModel {
    
    private double[] weights;
    private double bias;
    private double[] featureMeans;
    private double[] featureStds;
    private String modelType = "LINEAR_REGRESSION";
    private int trainingSamples = 0;
    private boolean loaded = false;

    @PostConstruct
    public void loadModel() {
        try {
            InputStream is = getClass()
                .getResourceAsStream("/maintenance_model.json");
            
            if (is == null) {
                System.err.println("maintenance_model.json not found!");
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);

            if (root.has("model_type")) {
                modelType = root.get("model_type").asText();
            }
            if (root.has("training_samples")) {
                trainingSamples = root.get("training_samples").asInt();
            }
            
            bias = root.get("bias").asDouble();
            
            JsonNode w = root.get("weights");
            weights = new double[w.size()];
            for (int i = 0; i < w.size(); i++) {
                weights[i] = w.get(i).asDouble();
            }
            
            JsonNode m = root.get("feature_means");
            featureMeans = new double[m.size()];
            for (int i = 0; i < m.size(); i++) {
                featureMeans[i] = m.get(i).asDouble();
            }
            
            JsonNode s = root.get("feature_stds");
            featureStds = new double[s.size()];
            for (int i = 0; i < s.size(); i++) {
                featureStds[i] = s.get(i).asDouble();
            }
            
            loaded = true;
            System.out.println("✅ Maintenance ML Model loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load maintenance model: " + e.getMessage());
        }
    }

    public double predictRisk(
            int kmSinceService, int daysSinceService,
            int fuelLevel, int engineCondition,
            int tireCondition, int brakeCondition,
            int oilLevel, double usagePressure) {

        if (!loaded) {
            return 0;
        }

        double[] raw = {
            kmSinceService / 15000.0,
            daysSinceService / 365.0,
            fuelLevel / 100.0,
            engineCondition / 100.0,
            tireCondition / 100.0,
            brakeCondition / 100.0,
            oilLevel / 100.0,
            Math.max(0.0, Math.min(1.0, usagePressure))
        };

        double result = bias;
        for (int i = 0; i < weights.length; i++) {
            double norm = (raw[i] - featureMeans[i]) / featureStds[i];
            result += weights[i] * norm;
        }
        
        return Math.max(0, Math.min(100, result));
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getModelType() {
        return modelType;
    }

    public int getTrainingSamples() {
        return trainingSamples;
    }
}
