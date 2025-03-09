package com.greenhouse.light.servicepublisher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator, ILightIntensityService {
    private ServiceRegistration<?> registration;
    private ExecutorService executorService;
    private volatile boolean running = true;

    // Define named zones
    private final String[] zones = {"Zone A - Tomatoes", "Zone B - Cucumbers", "Zone C - Peppers", "Zone D - Lettuce", "Zone E - Herbs"};
    private final Map<String, Integer> lightIntensityMap = new HashMap<>();

    @Override
    public void start(BundleContext bundleContext) {
        registration = bundleContext.registerService(ILightIntensityService.class, this, null);
        System.out.println("[Producer] Light Intensity Service Registered.");

        // Initialize all zones with default intensity
        for (String zone : zones) {
            lightIntensityMap.put(zone, 500); // Default intensity
        }

        // Start background task to generate light intensity data every 30 seconds
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::generateLightData);
    }

    private void generateLightData() {
        Random random = new Random();
        while (running) {
            try {
                for (String zone : zones) {
                    int newIntensity = random.nextInt(1000); // Generate random intensity for each zone
                    lightIntensityMap.put(zone, newIntensity);
                    //System.out.println("[Producer] " + zone + " Light Intensity: " + newIntensity + " lux");
                }

                Thread.sleep(30000); // Update every 30 seconds
            } catch (InterruptedException e) {
                System.err.println("[Producer] ERROR: Interrupted while generating light data.");
                break;
            }
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        registration.unregister();
        //System.out.println("[Producer] Light Intensity Service Stopped.");
    }

    @Override
    public Map<String, Integer> getLightIntensity() {
        return new HashMap<>(lightIntensityMap); // Return latest light intensity for all zones
    }
}
