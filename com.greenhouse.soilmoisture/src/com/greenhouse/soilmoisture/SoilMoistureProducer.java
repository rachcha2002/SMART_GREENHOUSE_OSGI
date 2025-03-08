package com.greenhouse.soilmoisture;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.soilmoisture.api.ISoilMoistureService;

public class SoilMoistureProducer implements ISoilMoistureService {
    private final Map<String, String> GREENHOUSE_ZONES = new HashMap<>();
    private final Map<String, Double> moistureLevels = new HashMap<>();
    private final Random random = new Random();

    public SoilMoistureProducer() {
        GREENHOUSE_ZONES.put("Zone-A", "Tomatoes");
        GREENHOUSE_ZONES.put("Zone-B", "Cucumbers");
        GREENHOUSE_ZONES.put("Zone-C", "Peppers");
        GREENHOUSE_ZONES.put("Zone-D", "Lettuce");
        GREENHOUSE_ZONES.put("Zone-E", "Herbs");

        generateMoistureLevels(); // Generate initial moisture levels
    }

    private void generateMoistureLevels() {
        for (String zone : GREENHOUSE_ZONES.keySet()) {
            moistureLevels.put(zone, 20 + random.nextDouble() * 60); // Random moisture (20-80%)
        }
    }

    @Override
    public Map<String, Double> getSoilMoistureLevels() {
        generateMoistureLevels(); // Simulate real-time data
        return moistureLevels;
    }
}
