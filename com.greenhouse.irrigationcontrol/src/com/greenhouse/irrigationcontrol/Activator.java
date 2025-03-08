package com.greenhouse.irrigationcontrol;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.greenhouse.report.IGreenhouseReporter;
import com.soilmoisture.api.ISoilMoistureService;

public class Activator implements BundleActivator {
    private IrrigationController irrigationController;
    private ServiceReference<ISoilMoistureService> soilServiceRef;
    private ServiceReference<IGreenhouseReporter> reporterRef;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[Activator] Starting IrrigationControl bundle...");
        
        // Try to get the reporter service first
        reporterRef = context.getServiceReference(IGreenhouseReporter.class);
        IGreenhouseReporter reporter = null;
        if (reporterRef != null) {
            reporter = context.getService(reporterRef);
            System.out.println("[IrrigationActivator] Connected to greenhouse reporter.");
        } else {
            System.out.println("[IrrigationActivator] Greenhouse reporter not available.");
        }
        
        // Get the soil moisture service
        soilServiceRef = context.getServiceReference(ISoilMoistureService.class);
        if (soilServiceRef != null) {
            ISoilMoistureService soilMoistureService = context.getService(soilServiceRef);
            irrigationController = new IrrigationController(soilMoistureService, reporter);
            irrigationController.startIrrigationCheck(); // Start monitoring
        } else {
            System.err.println("[Activator] Failed to start: SoilMoistureService not available!");
        }
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[Activator] Stopping IrrigationControl bundle...");
        
//        if (irrigationController != null) {
//            irrigationController.stopIrrigationCheck(); // Ensure it stops
//            irrigationController = null;
//        }
//        
//        if (soilServiceRef != null) {
//            context.ungetService(soilServiceRef);
//        }
//        
//        if (reporterRef != null) {
//            context.ungetService(reporterRef);
//        }
    }
}