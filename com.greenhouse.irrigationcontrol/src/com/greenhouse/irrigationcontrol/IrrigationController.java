package com.greenhouse.irrigationcontrol;

import java.util.Map;
import java.util.concurrent.*;

import com.greenhouse.report.IGreenhouseReporter;
import com.soilmoisture.api.ISoilMoistureService;

public class IrrigationController {
    private final ISoilMoistureService soilMoistureService;
    private final IGreenhouseReporter reporter;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private static final Map<String, Double> PLANT_MOISTURE_THRESHOLDS = Map.of(
        "Tomatoes", 45.00,
        "Cucumbers", 50.00,
        "Peppers", 40.00,
        "Lettuce", 55.00,
        "Herbs", 35.00
    );
    
    private static final Map<String, String> GREENHOUSE_ZONES = Map.of(
        "Zone-A", "Tomatoes",
        "Zone-B", "Cucumbers",
        "Zone-C", "Peppers",
        "Zone-D", "Lettuce",
        "Zone-E", "Herbs"
    );
    
    public IrrigationController(ISoilMoistureService soilMoistureService, IGreenhouseReporter reporter) {
        this.soilMoistureService = soilMoistureService;
        this.reporter = reporter;
    }
    
    public void checkAndIrrigate() {
        Map<String, Double> moistureData = soilMoistureService.getSoilMoistureLevels();
        System.out.println("\n--- Irrigation Report ---");
        
        System.out.println("============================================================");
        
        for (Map.Entry<String, Double> entry : moistureData.entrySet()) {
            String zone = entry.getKey();
            double moistureLevel = entry.getValue();
            String plantType = GREENHOUSE_ZONES.get(zone);
            double requiredMoisture = PLANT_MOISTURE_THRESHOLDS.getOrDefault(plantType, 40.00);
            String formattedMoisture = String.format("%.2f", moistureLevel);
            
            if (moistureLevel < requiredMoisture) {
                System.out.println(zone + 
                    " (" + plantType + ", Moisture: " + formattedMoisture + "%) -> Irrigating ");
                
                // Report the action
                if (reporter != null) {
                    reporter.recordAction("Irrigation System", 
                        "Irrigating " + zone + " (" + plantType + ") - moisture: " + formattedMoisture + "%");
                }
            } else {
                System.out.println(zone + 
                    " (" + plantType + ", Moisture: " + formattedMoisture + "%) -> No irrigation needed.");
                
                // Report the action
                if (reporter != null) {
                    reporter.recordAction("Irrigation System", 
                        "Skipped irrigation for " + zone + " - moisture sufficient (" + formattedMoisture + "%)");
                }
            }
        }
        System.out.println("============================================================");
    }
    
    // Run checkAndIrrigate() every 20 seconds continuously
    public void startIrrigationCheck() {
        System.out.println("[IrrigationController] Starting irrigation monitoring...");
        scheduler.scheduleAtFixedRate(
            this::checkAndIrrigate,
            0, 20, TimeUnit.SECONDS // Runs every 20 seconds
        );
    }
    
    public void stopIrrigationCheck() {
        scheduler.shutdown();
        System.out.println("[IrrigationController] Stopping irrigation monitoring.");
    }
}