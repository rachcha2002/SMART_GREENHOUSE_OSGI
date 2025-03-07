package com.greenhouse.light.servicesubscriber;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import com.greenhouse.light.servicepublisher.ILightIntensityService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator {
    private static final int LIGHT_THRESHOLD = 500;
    private ExecutorService executorService;
    private volatile boolean running = true;

    @Override
    public void start(BundleContext bundleContext) {
        System.out.println("[Consumer] Daylight Control System Started.");

        ServiceReference<ILightIntensityService> reference =
                bundleContext.getServiceReference(ILightIntensityService.class);
        ILightIntensityService lightService = bundleContext.getService(reference);

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> monitorLight(lightService));
    }

    private void monitorLight(ILightIntensityService lightService) {
        while (running) {
            try {
                double lightIntensity = lightService.getLightIntensity();
                System.out.println("[Consumer] Light Intensity: " + lightIntensity + " lux");

                if (lightIntensity > LIGHT_THRESHOLD) {
                    System.out.println("[Action] Closing Shades 🚪🌞");
                } else {
                    System.out.println("[Action] Opening Shades 🌥️🔄");
                }

                Thread.sleep(300000);
            } catch (InterruptedException e) {
                System.err.println("[Consumer] ERROR: Interrupted while monitoring light.");
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
        System.out.println("[Consumer] Daylight Control System Stopped.");
    }
}
