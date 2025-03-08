package com.greenhouse.light.servicesubscriber;

import com.greenhouse.light.servicepublisher.ILightIntensityService;
import com.greenhouse.report.IGreenhouseReporter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator {
    private ServiceReference<ILightIntensityService> lightServiceRef;
    private ServiceReference<IGreenhouseReporter> reporterRef;
    private ILightIntensityService lightIntensityService;
    private IGreenhouseReporter reporter;
    private ExecutorService executorService;
    private volatile boolean running = true;
    
    @Override
    public void start(BundleContext bundleContext) {
        // Try to get the reporter service first
        reporterRef = bundleContext.getServiceReference(IGreenhouseReporter.class);
        if (reporterRef != null) {
            reporter = bundleContext.getService(reporterRef);
            System.out.println("[LightConsumer] Connected to greenhouse reporter.");
        } else {
            System.out.println("[LightConsumer] Greenhouse reporter not available.");
        }
        
        // Get the light service
        lightServiceRef = bundleContext.getServiceReference(ILightIntensityService.class);
        if (lightServiceRef != null) {
            lightIntensityService = bundleContext.getService(lightServiceRef);
            System.out.println("[LightConsumer] Light Intensity Service Found.");
            
            // Start monitoring light intensity and adjusting lights
            executorService = Executors.newSingleThreadExecutor();
            executorService.execute(this::monitorLightIntensity);
        } else {
            System.err.println("[LightConsumer] ERROR: Light Intensity Service Not Found.");
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
                System.err.println("[LightConsumer] ERROR: Interrupted while monitoring light data.");
                break;
            }
        }
    }

    private void adjustLighting(String zone, int intensity) {
        if (intensity < 300) {
            System.out.println("[LightConsumer] " + zone + ": Increasing light brightness.");
            // Report the action to the reporter service if available
            if (reporter != null) {
                reporter.recordAction("Light System", "Increased brightness in " + zone + " (" + intensity + " lux)");
            }
        } else if (intensity > 700) {
            System.out.println("[LightConsumer] " + zone + ": Dimming lights.");
            // Report the action
            if (reporter != null) {
                reporter.recordAction("Light System", "Dimmed lights in " + zone + " (" + intensity + " lux)");
            }
        } else {
            System.out.println("[LightConsumer] " + zone + ": Lighting is optimal.");
            // Report the action
            if (reporter != null) {
                reporter.recordAction("Light System", "Maintained optimal lighting in " + zone + " (" + intensity + " lux)");
            }
        }
    }

    @Override
    public void stop(BundleContext bundleContext) {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (lightServiceRef != null) {
            bundleContext.ungetService(lightServiceRef);
        }
        
        if (reporterRef != null) {
            bundleContext.ungetService(reporterRef);
        }
        
        System.out.println("[LightConsumer] Light Intensity Consumer Stopped.");
    }
}