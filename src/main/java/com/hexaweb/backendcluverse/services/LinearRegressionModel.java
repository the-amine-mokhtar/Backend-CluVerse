package com.hexaweb.backendcluverse.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Component
public class LinearRegressionModel {
    
    private double[] weights;
    private double bias;
    private double[] featureMeans;
    private double[] featureStds;
    private int trainingSamples;
    private String modelType;
    private boolean loaded = false;

    @PostConstruct
    public void loadModel() {
        try {
            InputStream is = getClass()
                .getResourceAsStream("/traffic_model.json");
            
            if (is == null) {
                System.err.println("traffic_model.json not found!");
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);
            
            bias = root.get("bias").asDouble();
            trainingSamples = root.get("training_samples").asInt();
            modelType = root.get("model_type").asText();
            
            JsonNode w = root.get("weights");
            weights = new double[w.size()];
            for (int i = 0; i < w.size(); i++)
                weights[i] = w.get(i).asDouble();
            
            JsonNode m = root.get("feature_means");
            featureMeans = new double[m.size()];
            for (int i = 0; i < m.size(); i++)
                featureMeans[i] = m.get(i).asDouble();
            
            JsonNode s = root.get("feature_stds");
            featureStds = new double[s.size()];
            for (int i = 0; i < s.size(); i++)
                featureStds[i] = s.get(i).asDouble();
            
            loaded = true;
            System.out.println("✅ ML Model loaded: " + trainingSamples 
                + " training samples from GTFS Tunisia dataset");
                
        } catch (Exception e) {
            System.err.println("Failed to load ML model: " + e.getMessage());
        }
    }

    // Features order must match train_model.py:
    // [dep_hour, is_weekend, is_peak_morning, 
    //  is_peak_evening, is_lunch, is_friday_lunch]
    public double predict(double[] rawFeatures) {
        if (!loaded) return 60.0; // fallback if model not loaded
        
        double result = bias;
        for (int i = 0; i < weights.length; i++) {
            double normalized = (rawFeatures[i] - featureMeans[i]) 
                                / featureStds[i];
            result += weights[i] * normalized;
        }
        return Math.max(15, Math.min(300, result));
    }

    public boolean isLoaded() { return loaded; }
    public int getTrainingSamples() { return trainingSamples; }
    public String getModelType() { return modelType; }
}
