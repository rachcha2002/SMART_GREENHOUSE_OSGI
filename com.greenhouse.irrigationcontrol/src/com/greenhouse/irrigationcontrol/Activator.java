package com.greenhouse.irrigationcontrol;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.soilmoisture.api.ISoilMoistureService;

public class Activator implements BundleActivator {
    private IrrigationController irrigationController;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[Activator] Starting IrrigationControl bundle...");

        // Get the soil moisture service
        ServiceReference<ISoilMoistureService> serviceRef = context.getServiceReference(ISoilMoistureService.class);
        if (serviceRef != null) {
            ISoilMoistureService soilMoistureService = context.getService(serviceRef);
            irrigationController = new IrrigationController(soilMoistureService);
            irrigationController.startIrrigationCheck(); // Start monitoring
        } else {
            System.err.println("[Activator] Failed to start: SoilMoistureService not available!");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[Activator] Stopping IrrigationControl bundle...");

        if (irrigationController != null) {
            irrigationController.stopIrrigationCheck(); // Ensure it stops
            irrigationController = null;
        }
    }
}
