package com.greenhouse.climate.publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemperatureHumidityProducer implements TemperatureHumidityService {
    private Random random = new Random();
    private Map<String, ClimateData> zoneClimateData = new HashMap<>();
    private ExecutorService executorService;
    private volatile boolean running = true;
    
    // Just define zones, no crop types or optimal ranges
    private static final String[] GREENHOUSE_ZONES = {
        "Zone-A", "Zone-B", "Zone-C", "Zone-D", "Zone-E" 
    };
    
    public TemperatureHumidityProducer() {
        // Initialize with realistic climate data for each zone
        for (String zoneId : GREENHOUSE_ZONES) {
            // Start with reasonable temperature and humidity values
            double initialTemp = 23.0 + (random.nextDouble() * 6) - 3; // 20-26°C range initially
            double initialHumidity = 65.0 + (random.nextDouble() * 20) - 10; // 55-75% range initially
            
            // Create climate data for zone
            ClimateData data = new ClimateData(zoneId, initialTemp, initialHumidity);
            zoneClimateData.put(zoneId, data);
        }
        
        System.out.println("[TemperatureHumidityProducer] Initialized sensors for " + GREENHOUSE_ZONES.length + " zones");
    }
    
    public void start() {
        // Start a thread to periodically update climate data
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::updateClimateData);
        System.out.println("[TemperatureHumidityProducer] Service started. Generating data every 30 seconds.");
    }
    
    private void updateClimateData() {
        while (running) {
            try {
                // Update climate data for each zone
                for (String zoneId : GREENHOUSE_ZONES) {
                    updateZoneClimate(zoneId);
                }
                
                // Wait before the next update
                Thread.sleep(30000); // Update every 30 seconds (changed from 10 seconds)
            } catch (InterruptedException e) {
                System.out.println("[TemperatureHumidityProducer] Climate monitoring interrupted");
                running = false;
                break;
            }
        }
    }
    
    private void updateZoneClimate(String zoneId) {
        ClimateData currentData = zoneClimateData.get(zoneId);
        
        // Get current values
        double currentTemp = currentData.getTemperature();
        double currentHumidity = currentData.getHumidity();
        
        // Simulate climate fluctuations
        double tempDelta = (random.nextDouble() * 0.8) - 0.4; // -0.4 to +0.4°C
        double humidityDelta = (random.nextDouble() * 2.0) - 1.0; // -1.0 to +1.0%
        
        // Periodically simulate more significant changes (weather events)
        if (random.nextInt(20) == 0) { // 5% chance of weather event
            if (random.nextBoolean()) {
                // Sunny day event (no logging)
                tempDelta += 2.0;
                humidityDelta -= 4.0;
            } else {
                // Rainy day event (no logging)
                tempDelta -= 1.5;
                humidityDelta += 6.0;
            }
        }
        
        // Calculate new values
        double newTemp = currentTemp + tempDelta;
        double newHumidity = currentHumidity + humidityDelta;
        
        // Keep humidity within realistic bounds (0-100%)
        newHumidity = Math.max(0, Math.min(100, newHumidity));
        
        // Update the climate data for this zone
        ClimateData newData = new ClimateData(zoneId, newTemp, newHumidity);
        zoneClimateData.put(zoneId, newData);
        
        // No logging here - all logging will be done in the consumer
    }
    
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        System.out.println("[TemperatureHumidityProducer] Service stopped");
    }

    @Override
    public Map<String, ClimateData> getAllZonesClimateData() {
        return new HashMap<>(zoneClimateData);  // Return a copy to prevent modification
    }
    
    @Override
    public ClimateData getZoneClimateData(String zoneId) {
        return zoneClimateData.get(zoneId);
    }
    
    @Override
    public String[] getAvailableZones() {
        return GREENHOUSE_ZONES;
    }
}