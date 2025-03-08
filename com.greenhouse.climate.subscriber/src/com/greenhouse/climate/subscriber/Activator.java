package com.greenhouse.climate.subscriber;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import com.greenhouse.climate.publisher.TemperatureHumidityService;
import com.greenhouse.report.IGreenhouseReporter;

public class Activator implements BundleActivator {
    private ServiceReference<TemperatureHumidityService> serviceReference;
    private ServiceReference<IGreenhouseReporter> reporterRef;
    private CoolingHeatingManager climateManager;
    private IGreenhouseReporter reporter;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[ClimateSubscriber] Starting service...");
        
        // First, try to get the reporter service
        reporterRef = context.getServiceReference(IGreenhouseReporter.class);
        if (reporterRef != null) {
            reporter = context.getService(reporterRef);
            System.out.println("[ClimateSubscriber] Connected to greenhouse reporter.");
        } else {
            System.out.println("[ClimateSubscriber] Greenhouse reporter not available.");
        }
        
        // Get a reference to the climate service
        serviceReference = context.getServiceReference(TemperatureHumidityService.class);
        
        if (serviceReference != null) {
            // Get the service
            TemperatureHumidityService climateService = context.getService(serviceReference);
            
            if (climateService != null) {
                // Create the climate manager with reporter
                climateManager = new CoolingHeatingManager(climateService, reporter);
                
                // Start the climate manager
                climateManager.start();
                
                System.out.println("[ClimateSubscriber] Successfully connected to Climate Publisher");
            } else {
                System.err.println("[ClimateSubscriber] Climate service unavailable");
            }
        } else {
            System.err.println("[ClimateSubscriber] Climate service reference not found");
        }
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[ClimateSubscriber] Stopping service...");
        
        // Stop the climate manager
        if (climateManager != null) {
            climateManager.stop();
        }
        
        // Unget the services
        if (serviceReference != null) {
            context.ungetService(serviceReference);
        }
        
        if (reporterRef != null) {
            context.ungetService(reporterRef);
        }
        
        System.out.println("[ClimateSubscriber] Service stopped");
    }
}