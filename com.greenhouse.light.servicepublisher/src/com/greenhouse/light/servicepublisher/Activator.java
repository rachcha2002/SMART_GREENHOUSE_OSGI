package com.greenhouse.light.servicepublisher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator, ILightIntensityService {
    private ServiceRegistration<?> registration;
    private ExecutorService executorService;
    private volatile boolean running = true;
    private int latestLightIntensity = 500;  // Default value

    @Override
    public void start(BundleContext bundleContext) {
        registration = bundleContext.registerService(ILightIntensityService.class, this, null);
        System.out.println("[Producer] Light Intensity Service Registered.");

        // Start background task to generate light intensity data every 30 seconds
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::generateLightData);
    }

    private void generateLightData() {
        Random random = new Random();
        while (running) {
            try {
                latestLightIntensity = random.nextInt(1000);  // Generate random intensity
                System.out.println("[Producer] Light Intensity Updated: " + latestLightIntensity + " lux");

                Thread.sleep(30000);  // Update every 30 seconds
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
        System.out.println("[Producer] Light Intensity Service Stopped.");
    }

    @Override
    public double getLightIntensity() {
        return latestLightIntensity;  // Return the latest light intensity value
    }
}
