package com.greenhouse.climate.publisher;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemperatureHumidityProducer implements TemperatureHumidityService {
    private Random random = new Random();
    private volatile ClimateData currentClimateData;
    private ExecutorService executorService;
    private volatile boolean running = true;
    
    // Define optimal temperature and humidity ranges for greenhouse
    private static final double MIN_OPTIMAL_TEMP = 21.0;
    private static final double MAX_OPTIMAL_TEMP = 26.0;
    private static final double MIN_OPTIMAL_HUMIDITY = 60.0;
    private static final double MAX_OPTIMAL_HUMIDITY = 80.0;
    
    // Greenhouse zones
    private static final String[] GREENHOUSE_ZONES = {
        "Zone A - Tomatoes", "Zone B - Cucumbers", "Zone C - Peppers", 
        "Zone D - Lettuce", "Zone E - Herbs"
    };
    
    private String currentZone;
    
    public TemperatureHumidityProducer() {
        // Initialize with random climate data
        double initialTemp = 23.0 + (random.nextDouble() * 5) - 2.5; // 20.5-25.5°C
        double initialHumidity = 70.0 + (random.nextDouble() * 20) - 10; // 60-80%
        currentClimateData = new ClimateData(initialTemp, initialHumidity);
        currentZone = GREENHOUSE_ZONES[random.nextInt(GREENHOUSE_ZONES.length)];
    }
    
    public void start() {
        // Start a thread to periodically update climate data
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::updateClimateData);
        System.out.println("[TemperatureHumidityProducer] Service started. Monitoring greenhouse climate.");
    }
    
    private void updateClimateData() {
        while (running) {
            try {
                // Update current zone randomly occasionally
                if (random.nextInt(10) == 0) { // 10% chance to change zone
                    currentZone = GREENHOUSE_ZONES[random.nextInt(GREENHOUSE_ZONES.length)];
                }
                
                // Get current values
                double currentTemp = currentClimateData.getTemperature();
                double currentHumidity = currentClimateData.getHumidity();
                
                // Simulate climate fluctuations (more realistic than completely random)
                double tempDelta = (random.nextDouble() * 1.0) - 0.5; // -0.5 to +0.5°C
                double humidityDelta = (random.nextDouble() * 3.0) - 1.5; // -1.5 to +1.5%
                
                // Periodically simulate more significant changes (weather events)
                if (random.nextInt(20) == 0) { // 5% chance of weather event
                    if (random.nextBoolean()) {
                        System.out.println("[TemperatureHumidityProducer] Simulating sunny day temperature spike");
                        tempDelta += 3.0; // Temperature spike
                        humidityDelta -= 5.0; // Humidity drop
                    } else {
                        System.out.println("[TemperatureHumidityProducer] Simulating rainy day humidity spike");
                        tempDelta -= 2.0; // Temperature drop
                        humidityDelta += 8.0; // Humidity spike
                    }
                }
                
                // Calculate new values
                double newTemp = currentTemp + tempDelta;
                double newHumidity = currentHumidity + humidityDelta;
                
                // Keep humidity within realistic bounds (0-100%)
                newHumidity = Math.max(0, Math.min(100, newHumidity));
                
                // Update the current climate data
                currentClimateData = new ClimateData(newTemp, newHumidity);
                
                // Log the new climate data with the zone
                System.out.println("[TemperatureHumidityProducer] " + currentZone + " - " + currentClimateData);
                
                // Analyze if conditions are optimal
                if (newTemp < MIN_OPTIMAL_TEMP || newTemp > MAX_OPTIMAL_TEMP ||
                    newHumidity < MIN_OPTIMAL_HUMIDITY || newHumidity > MAX_OPTIMAL_HUMIDITY) {
                    System.out.println("[TemperatureHumidityProducer] ⚠️ Warning: Climate conditions outside optimal range");
                }
                
                // Wait before the next update
                Thread.sleep(10000); // Update every 10 seconds
            } catch (InterruptedException e) {
                System.out.println("[TemperatureHumidityProducer] Climate monitoring interrupted");
                running = false;
                break;
            }
        }
    }
    
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        System.out.println("[TemperatureHumidityProducer] Service stopped");
    }
    
    @Override
    public ClimateData getClimateData() {
        return currentClimateData;
    }
    
    public String getCurrentZone() {
        return currentZone;
    }
}