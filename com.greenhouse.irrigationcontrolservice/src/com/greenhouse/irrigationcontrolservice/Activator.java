package com.greenhouse.irrigationcontrolservice;
import com.greenhouse.soilmoistureservice.SoilMoistureProducer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import com.greenhouse.report.IGreenhouseReporter;

public class Activator implements BundleActivator {
    private IrrigationController irrigationController;
    private ServiceReference<IGreenhouseReporter> reporterRef;
    private ServiceReference<?> soilMoistureRef;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[IrrigationActivator] Starting IrrigationControl bundle...");
        
        // First, try to get the reporter service
        reporterRef = context.getServiceReference(IGreenhouseReporter.class);
        IGreenhouseReporter reporter = null;
        if (reporterRef != null) {
            reporter = context.getService(reporterRef);
            System.out.println("[IrrigationActivator] Connected to greenhouse reporter.");
        } else {
            System.out.println("[IrrigationActivator] Greenhouse reporter not available.");
        }
        
        // Retrieve the SoilMoistureProducer service by its concrete class name.
        soilMoistureRef = context.getServiceReference(SoilMoistureProducer.class.getName());
        if (soilMoistureRef != null) {
            SoilMoistureProducer producer = (SoilMoistureProducer) context.getService(soilMoistureRef);
            irrigationController = new IrrigationController(producer, reporter);
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
        
        // Unget the services
        if (soilMoistureRef != null) {
            context.ungetService(soilMoistureRef);
        }
        
        if (reporterRef != null) {
            context.ungetService(reporterRef);
        }
        
        System.out.println("[IrrigationActivator] Irrigation control bundle stopped.");
    }
}