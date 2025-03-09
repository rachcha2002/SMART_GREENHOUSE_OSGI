package com.greenhouse.pest.servicepublisher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PestServicePublishImpl implements PestServicePublish {

    private ExecutorService executorService;
    private volatile boolean running = true;
    private String latestPestStatus = "No pests detected.";
    private Random random = new Random();

    // Greenhouse zones with crop types
    private static final Map<String, String> GREENHOUSE_ZONES = new HashMap<>();
    
    // Camera IDs for each zone
    private static final Map<String, String[]> CAMERA_IDS = new HashMap<>();

    // Sample pest issues
    private static final String[] PESTS = {
        "Aphids", "Whiteflies", "Spider Mites", "Mealybugs", "Leafhoppers", 
        "Thrips", "Scale insects", "Ants", "Caterpillars", "Root-Knot Nematodes",
        "Flea Beetles", "Cutworms", "Japanese Beetles", "Leaf Miners", "Squash Bugs",
        "Colorado Potato Beetles", "White Grubs", "Stink Bugs", "Red Palm Weevil larvae", "Tomato Hornworms"
    };

    static {
        GREENHOUSE_ZONES.put("Zone-A", "Tomatoes");
        GREENHOUSE_ZONES.put("Zone-B", "Cucumbers");
        GREENHOUSE_ZONES.put("Zone-C", "Peppers");
        GREENHOUSE_ZONES.put("Zone-D", "Lettuce");
        GREENHOUSE_ZONES.put("Zone-E", "Herbs");
        
        // Assigning 5 camera IDs for each zone
        CAMERA_IDS.put("Zone-A", new String[]{"Camera-1", "Camera-2", "Camera-3", "Camera-4", "Camera-5"});
        CAMERA_IDS.put("Zone-B", new String[]{"Camera-1", "Camera-2", "Camera-3", "Camera-4", "Camera-5"});
        CAMERA_IDS.put("Zone-C", new String[]{"Camera-1", "Camera-2", "Camera-3", "Camera-4", "Camera-5"});
        CAMERA_IDS.put("Zone-D", new String[]{"Camera-1", "Camera-2", "Camera-3", "Camera-4", "Camera-5"});
        CAMERA_IDS.put("Zone-E", new String[]{"Camera-1", "Camera-2", "Camera-3", "Camera-4", "Camera-5"});
    }

    public void start() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::updatePestData);
    }

    private void updatePestData() {
        while (running) {
            try {
                String zone = getRandomZone();
                String crop = GREENHOUSE_ZONES.get(zone);
                String pest = getRandomPest();
                String cameraID = getRandomCameraID(zone);
                String timestamp = getFormattedTimestamp();

                // Formatting the output in a more readable console pattern
                latestPestStatus = formatPestStatus(zone, crop, cameraID, pest, timestamp);
                
                // Pass the formatted pest status
                notifySubscribers();

                Thread.sleep(30000);
            } catch (InterruptedException e) {
                System.err.println("[PestDetectionCamera] ERROR: Interrupted while generating pest data.");
                break;
            }
        }
    }

    private void notifySubscribers() {
        // Placeholder for notifying subscribers
        //System.out.println(latestPestStatus); // Example of passing the formatted output
    }

    private String getRandomZone() {
        Object[] zones = GREENHOUSE_ZONES.keySet().toArray();
        return (String) zones[random.nextInt(zones.length)];
    }

    private String getRandomCameraID(String zone) {
        String[] cameras = CAMERA_IDS.get(zone);
        return cameras[random.nextInt(cameras.length)];
    }

    private String getRandomPest() {
        return PESTS[random.nextInt(PESTS.length)];
    }

    private String getFormattedTimestamp() {
        // Get the current timestamp and format it up to the second
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    private String formatPestStatus(String zone, String crop, String cameraID, String pest, String timestamp) {
        // Use a clean console log pattern with nice spacing
        return String.format(
            "==============================================\n" +
            "           Pest Detection Report\n" +
            "==============================================\n" +
            "  Timestamp        : %s\n" +
            "  Greenhouse Zone  : %s\n" +
            "  Crop             : %s\n" +
            "  Camera ID        : %s\n" +
            "  Detected Pest    : %s\n" +
            "==============================================\n",
            timestamp, zone, crop, cameraID, pest);
    }

    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public String detectPests() {
        return latestPestStatus;
    }
}
