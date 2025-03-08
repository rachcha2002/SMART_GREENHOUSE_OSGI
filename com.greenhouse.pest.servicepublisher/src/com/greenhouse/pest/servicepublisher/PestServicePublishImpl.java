package com.greenhouse.pest.servicepublisher;

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
                String timestamp = java.time.LocalDateTime.now().toString();

                latestPestStatus = String.format("Time: %s, Zone: %s, Crop: %s, Pest: %s", timestamp, zone, crop, pest);
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
    }

    private String getRandomZone() {
        Object[] zones = GREENHOUSE_ZONES.keySet().toArray();
        return (String) zones[random.nextInt(zones.length)];
    }

    private String getRandomPest() {
        return PESTS[random.nextInt(PESTS.length)];
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
