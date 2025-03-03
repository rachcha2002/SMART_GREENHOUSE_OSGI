package com.greenhouse.pest.servicepublisher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator, PestServicePublish {

    private ServiceRegistration<?> registration;
    private ExecutorService executorService;
    private volatile boolean running = true;
    private String latestPestStatus = "No pests detected.";  // Stores the latest pest data

    // Sample greenhouse data
    private static final String[] GREENHOUSE_NUMBERS = {
        "GH001", "GH002", "GH003", "GH004", "GH005", 
        "GH006", "GH007", "GH008", "GH009", "GH010", 
        "GH011", "GH012", "GH013", "GH014", "GH015", 
        "GH016", "GH017", "GH018", "GH019", "GH020"
    };

    // Sample crop types
    private static final String[] CROPS = {
        "Potato plants", "Tomato plants", "Cucumber plants", "Strawberry plants",
        "Lettuce plants", "Bell Pepper plants", "Mango trees", "Cabbage plants",
        "Eggplant plants", "Carrot plants", "Broccoli plants", "Spinach plants",
        "Green Beans", "Parsley plants", "Zucchini plants", "Potatoes", "Corn plants",
        "Peppers", "Palm trees", "Tomato plants"
    };

    // Sample pest issues
    private static final String[] PESTS = {
        "Aphids", "Whiteflies", "Spider Mites", "Mealybugs", "Leafhoppers", 
        "Thrips", "Scale insects", "Ants", "Caterpillars", "Root-Knot Nematodes",
        "Flea Beetles", "Cutworms", "Japanese Beetles", "Leaf Miners", "Squash Bugs",
        "Colorado Potato Beetles", "White Grubs", "Stink Bugs", "Red Palm Weevil larvae", "Tomato Hornworms"
    };

    // Camera numbers
    private static final String[] CAMERA_NUMBERS = {
        "C001", "C002", "C003", "C004", "C005", "C006", "C007", "C008", "C009", "C010", 
        "C011", "C012", "C013", "C014", "C015", "C016", "C017", "C018", "C019", "C020"
    };

    @Override
    public void start(BundleContext bundleContext) {
        registration = bundleContext.registerService(PestServicePublish.class, this, null);
        System.out.println("[PestDetectionCamera] Service Registered.");

        // Start background task to update pest data every 30 seconds
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::updatePestData);
    }

    private void updatePestData() {
        Random random = new Random();
        while (running) {
            try {
                // Randomly select greenhouse, crop, pest, and camera number
                String greenhouse = GREENHOUSE_NUMBERS[random.nextInt(GREENHOUSE_NUMBERS.length)];
                String crop = CROPS[random.nextInt(CROPS.length)];
                String pest = PESTS[random.nextInt(PESTS.length)];
                String camera = CAMERA_NUMBERS[random.nextInt(CAMERA_NUMBERS.length)];

                // Construct the pest status data
                latestPestStatus = String.format("Greenhouse: %s, Crop: %s, Pest: %s, Camera: %s", greenhouse, crop, pest, camera);

                // Notify subscribers about the new pest data
                notifySubscribers();

                // Wait for 30 seconds before updating again
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                System.err.println("[PestDetectionCamera] ERROR: Interrupted while generating pest data.");
                break;
            }
        }
    }

    private void notifySubscribers() {
        // In the publisher, we don't print the data here, only send it.
        // This can be where you'd send data to subscribers, depending on the system you're using.
    }

    @Override
    public void stop(BundleContext bundleContext) {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        registration.unregister();
        System.out.println("[PestDetectionCamera] Service Stopped.");
    }

    @Override
    public String detectPests() {
        return latestPestStatus;  // Return the most recent pest detection result
    }
}
