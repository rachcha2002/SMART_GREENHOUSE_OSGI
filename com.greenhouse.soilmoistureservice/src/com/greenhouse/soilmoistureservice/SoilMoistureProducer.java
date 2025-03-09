package com.greenhouse.soilmoistureservice;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SoilMoistureProducer {
    private final Map<String, String> GREENHOUSE_ZONES = new HashMap<>();
    private final Map<String, Double> moistureLevels = new HashMap<>();
    private final Random random = new Random();

    public SoilMoistureProducer() {
        // Define zones and corresponding crops
        GREENHOUSE_ZONES.put("Zone-A", "Tomatoes");
        GREENHOUSE_ZONES.put("Zone-B", "Cucumbers");
        GREENHOUSE_ZONES.put("Zone-C", "Peppers");
        GREENHOUSE_ZONES.put("Zone-D", "Lettuce");
        GREENHOUSE_ZONES.put("Zone-E", "Herbs");

        generateMoistureLevels();
    }

    private void generateMoistureLevels() {
        for (String zone : GREENHOUSE_ZONES.keySet()) {
            // Generate random moisture between 20% and 80%
            moistureLevels.put(zone, 20 + random.nextDouble() * 60);
        }
    }

    // Returns updated moisture levels (simulated)
    public Map<String, Double> getSoilMoistureLevels() {
        generateMoistureLevels();
        return moistureLevels;
    }
}
