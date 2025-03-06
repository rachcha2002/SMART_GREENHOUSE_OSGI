package com.greenhouse.irrigationcontrol;

import com.soilmoisture.api.IIrrigationControlService;
import com.soilmoisture.api.ISoilMoistureService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator, IIrrigationControlService {

    private ServiceReference<ISoilMoistureService> moistureRef;
    private ISoilMoistureService moistureService;

    private ServiceRegistration<IIrrigationControlService> irrigationRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[IrrigationController] Bundle Starting...");

        // 1. Look up the SoilMoisture service
        moistureRef = context.getServiceReference(ISoilMoistureService.class);
        if (moistureRef != null) {
            moistureService = context.getService(moistureRef);
            System.out.println("[IrrigationController] SoilMoistureService found!");

            // 2. Use the service right away (dummy example)
            double moistureValue = moistureService.getSoilMoisture();
            controlIrrigation(moistureValue);
        } else {
            System.out.println("[IrrigationController] SoilMoistureService NOT found!");
        }

        // 3. Register ourselves as IIrrigationControlService if needed
        irrigationRegistration = context.registerService(
            IIrrigationControlService.class, this, null
        );

        System.out.println("[IrrigationController] Bundle Started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Unregister irrigation service
        if (irrigationRegistration != null) {
            irrigationRegistration.unregister();
        }

        // Release the SoilMoisture service
        if (moistureRef != null) {
            context.ungetService(moistureRef);
        }

        System.out.println("[IrrigationController] Bundle Stopped");
    }

    @Override
    public void controlIrrigation(double moistureLevel) {
        System.out.println("[IrrigationController] Checking moisture level: " + moistureLevel + "%");
        if (moistureLevel < 30.0) {
            System.out.println("[IrrigationController] Moisture too low. Activating irrigation!");
        } else {
            System.out.println("[IrrigationController] Moisture is sufficient. No irrigation needed.");
        }
    }
}
