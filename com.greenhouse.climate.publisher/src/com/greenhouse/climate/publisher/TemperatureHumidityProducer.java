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
    
    // Define optimal temperature and humidity ranges for different crops
    private static final Map<String, double[]> CROP_OPTIMAL_TEMPS = new HashMap<>();
    private static final Map<String, double[]> CROP_OPTIMAL_HUMIDITY = new HashMap<>();
    
    // Greenhouse zones with crop types
    private static final Map<String, String> GREENHOUSE_ZONES = new HashMap<>();
    
    // Initialize zone and crop data
    static {
        // Define zones and crops
        GREENHOUSE_ZONES.put("Zone-A", "Tomatoes");
        GREENHOUSE_ZONES.put("Zone-B", "Cucumbers");
        GREENHOUSE_ZONES.put("Zone-C", "Peppers");
        GREENHOUSE_ZONES.put("Zone-D", "Lettuce");
        GREENHOUSE_ZONES.put("Zone-E", "Herbs");
        
        // Define optimal temperature ranges for each crop [min, max]
        CROP_OPTIMAL_TEMPS.put("Tomatoes", new double[]{21.0, 27.0});
        CROP_OPTIMAL_TEMPS.put("Cucumbers", new double[]{23.0, 28.0});
        CROP_OPTIMAL_TEMPS.put("Peppers", new double[]{22.0, 26.0});
        CROP_OPTIMAL_TEMPS.put("Lettuce", new double[]{15.0, 22.0});
        CROP_OPTIMAL_TEMPS.put("Herbs", new double[]{18.0, 24.0});
        
        // Define optimal humidity ranges for each crop [min, max]
        CROP_OPTIMAL_HUMIDITY.put("Tomatoes", new double[]{65.0, 80.0});
        CROP_OPTIMAL_HUMIDITY.put("Cucumbers", new double[]{70.0, 85.0});
        CROP_OPTIMAL_HUMIDITY.put("Peppers", new double[]{65.0, 75.0});
        CROP_OPTIMAL_HUMIDITY.put("Lettuce", new double[]{60.0, 70.0});
        CROP_OPTIMAL_HUMIDITY.put("Herbs", new double[]{55.0, 70.0});
    }
    
    public TemperatureHumidityProducer() {
        // Initialize with realistic climate data for each zone
        for (String zoneId : GREENHOUSE_ZONES.keySet()) {
            String cropType = GREENHOUSE_ZONES.get(zoneId);
            double[] tempRange = CROP_OPTIMAL_TEMPS.get(cropType);
            double[] humidityRange = CROP_OPTIMAL_HUMIDITY.get(cropType);
            
            // Generate initial values within optimal ranges
            double initialTemp = tempRange[0] + (random.nextDouble() * (tempRange[1] - tempRange[0]));
            double initialHumidity = humidityRange[0] + (random.nextDouble() * (humidityRange[1] - humidityRange[0]));
            
            // Create climate data for zone
            ClimateData data = new ClimateData(zoneId, initialTemp, initialHumidity);
            zoneClimateData.put(zoneId, data);
        }
        
        System.out.println("[TemperatureHumidityProducer] Initialized sensors for " + GREENHOUSE_ZONES.size() + " zones");
    }
    
    public void start() {
        // Start a thread to periodically update climate data
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::updateClimateData);
        System.out.println("[TemperatureHumidityProducer] Service started. Monitoring all greenhouse zones.");
    }
    
    private void updateClimateData() {
        while (running) {
            try {
                // Update climate data for each zone
                for (String zoneId : GREENHOUSE_ZONES.keySet()) {
                    updateZoneClimate(zoneId);
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
    
    private void updateZoneClimate(String zoneId) {
        ClimateData currentData = zoneClimateData.get(zoneId);
        String cropType = GREENHOUSE_ZONES.get(zoneId);
        
        // Get current values
        double currentTemp = currentData.getTemperature();
        double currentHumidity = currentData.getHumidity();
        
        // Simulate climate fluctuations (more realistic than completely random)
        double tempDelta = (random.nextDouble() * 0.8) - 0.4; // -0.4 to +0.4°C
        double humidityDelta = (random.nextDouble() * 2.0) - 1.0; // -1.0 to +1.0%
        
        // Periodically simulate more significant changes (weather events)
        if (random.nextInt(20) == 0) { // 5% chance of weather event
            if (random.nextBoolean()) {
                System.out.println("[Sensor-" + zoneId + "] Simulating sunny day temperature spike");
                tempDelta += 2.0; // Temperature spike
                humidityDelta -= 4.0; // Humidity drop
            } else {
                System.out.println("[Sensor-" + zoneId + "] Simulating rainy day humidity spike");
                tempDelta -= 1.5; // Temperature drop
                humidityDelta += 6.0; // Humidity spike
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
        
        // Get optimal ranges for this crop
        double[] tempRange = CROP_OPTIMAL_TEMPS.get(cropType);
        double[] humidityRange = CROP_OPTIMAL_HUMIDITY.get(cropType);
        
        // Log the new climate data with crop information
        System.out.println("[Sensor-" + zoneId + "] " + cropType + " - " + newData);
        
        // Analyze if conditions are optimal
        if (newTemp < tempRange[0] || newTemp > tempRange[1] ||
            newHumidity < humidityRange[0] || newHumidity > humidityRange[1]) {
            System.out.println("[Sensor-" + zoneId + "] ⚠️ Warning: Climate conditions outside optimal range for " + cropType);
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
    public Map<String, ClimateData> getAllZonesClimateData() {
        return new HashMap<>(zoneClimateData);  // Return a copy to prevent modification
    }
    
    @Override
    public ClimateData getZoneClimateData(String zoneId) {
        return zoneClimateData.get(zoneId);
    }
    
    @Override
    public String[] getAvailableZones() {
        return GREENHOUSE_ZONES.keySet().toArray(new String[0]);
    }
}