package com.greenhouse.irrigationcontrolservice;

import com.greenhouse.soilmoistureservice.SoilMoistureProducer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {
    private IrrigationController irrigationController;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[IrrigationActivator] Starting IrrigationControl bundle...");

        // Retrieve the SoilMoistureProducer service by its concrete class name.
        ServiceReference<?> serviceRef = context.getServiceReference(SoilMoistureProducer.class.getName());
        if (serviceRef != null) {
            SoilMoistureProducer producer = (SoilMoistureProducer) context.getService(serviceRef);
            irrigationController = new IrrigationController(producer);
            irrigationController.startIrrigationCheck();
        } else {
            System.err.println("[IrrigationActivator] ERROR: SoilMoistureProducer service not available!");
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[IrrigationActivator] Stopping IrrigationControl bundle...");
        if (irrigationController != null) {
            irrigationController.stopIrrigationCheck();
            irrigationController = null;
        }
    }
}
