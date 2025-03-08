package com.greenhouse.light.servicesubscriber;

import com.greenhouse.light.servicepublisher.ILightIntensityService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator {
    private ServiceReference<ILightIntensityService> serviceReference;
    private ILightIntensityService lightIntensityService;
    private ExecutorService executorService;
    private volatile boolean running = true;

    @Override
    public void start(BundleContext bundleContext) {
        serviceReference = bundleContext.getServiceReference(ILightIntensityService.class);
        if (serviceReference != null) {
            lightIntensityService = bundleContext.getService(serviceReference);
            System.out.println("[Consumer] Light Intensity Service Found.");

            // Start monitoring light intensity and adjusting lights
            executorService = Executors.newSingleThreadExecutor();
            executorService.execute(this::monitorLightIntensity);
        } else {
            System.err.println("[Consumer] ERROR: Light Intensity Service Not Found.");
        }
    }

    private void monitorLightIntensity() {
        while (running) {
            try {
                if (lightIntensityService != null) {
                    Map<String, Integer> intensityData = lightIntensityService.getLightIntensity();
                    for (Map.Entry<String, Integer> entry : intensityData.entrySet()) {
                        String zone = entry.getKey();
                        int intensity = entry.getValue();
                        adjustLighting(zone, intensity);
                    }
                }
                Thread.sleep(30000); // Check every 30 seconds
            } catch (InterruptedException e) {
                System.err.println("[Consumer] ERROR: Interrupted while monitoring light data.");
                break;
            }
        }
    }

    private void adjustLighting(String zone, int intensity) {
        if (intensity < 300) {
            System.out.println("[Consumer] " + zone + ": Increasing light brightness.");
        } else if (intensity > 700) {
            System.out.println("[Consumer] " + zone + ": Dimming lights.");
        } else {
            System.out.println("[Consumer] " + zone + ": Lighting is optimal.");
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        if (serviceReference != null) {
            bundleContext.ungetService(serviceReference);
        }
        System.out.println("[Consumer] Light Intensity Consumer Stopped.");
    }
}
