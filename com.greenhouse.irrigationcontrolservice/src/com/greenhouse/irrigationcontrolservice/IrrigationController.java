package com.greenhouse.irrigationcontrolservice;

import java.util.Map;
import java.util.concurrent.*;

import com.greenhouse.soilmoistureservice.SoilMoistureProducer;

public class IrrigationController {
    private final SoilMoistureProducer producer;
    // Use a ScheduledExecutorService with daemon threads so it runs in the background.
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Define thresholds for each crop type.
    private static final Map<String, Double> PLANT_MOISTURE_THRESHOLDS = Map.of(
        "Tomatoes", 45.00,
        "Cucumbers", 50.00,
        "Peppers", 40.00,
        "Lettuce", 55.00,
        "Herbs", 35.00
    );

    // Map zones to crops.
    private static final Map<String, String> GREENHOUSE_ZONES = Map.of(
        "Zone-A", "Tomatoes",
        "Zone-B", "Cucumbers",
        "Zone-C", "Peppers",
        "Zone-D", "Lettuce",
        "Zone-E", "Herbs"
    );

    public IrrigationController(SoilMoistureProducer producer) {
        this.producer = producer;
    }

    public void checkAndIrrigate() {
        Map<String, Double> moistureData = producer.getSoilMoistureLevels();
        System.out.println("\n--- Irrigation Report ---");
        System.out.println("============================================================");
        
        for (Map.Entry<String, Double> entry : moistureData.entrySet()) {
            String zone = entry.getKey();
            double moistureLevel = entry.getValue();
            String plantType = GREENHOUSE_ZONES.get(zone);
            double requiredMoisture = PLANT_MOISTURE_THRESHOLDS.getOrDefault(plantType, 40.00);
            String formattedMoisture = String.format("%.2f", moistureLevel);

            if (moistureLevel < requiredMoisture) {
                System.out.println(zone + " (" + plantType + ", Moisture: " 
                    + formattedMoisture + "%) -> Irrigating");
            } else {
                System.out.println(zone + " (" + plantType + ", Moisture: " 
                    + formattedMoisture + "%) -> No irrigation needed.");
            }
        }
        System.out.println("============================================================");
    }

    // Schedule the irrigation check every 20 seconds continuously.
    public void startIrrigationCheck() {
        System.out.println("[IrrigationController] Starting irrigation monitoring...");
        scheduler.scheduleAtFixedRate(this::checkAndIrrigate, 0, 20, TimeUnit.SECONDS);
    }

    public void stopIrrigationCheck() {
        scheduler.shutdownNow();
        System.out.println("[IrrigationController] Stopping irrigation monitoring.");
    }
}
