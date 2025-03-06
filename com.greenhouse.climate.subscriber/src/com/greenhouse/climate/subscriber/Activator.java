package com.greenhouse.climate.subscriber;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.greenhouse.climate.publisher.TemperatureHumidityService;

public class Activator implements BundleActivator {
    private ServiceReference<TemperatureHumidityService> serviceReference;
    private CoolingHeatingManager climateManager;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[ClimateSubscriber] Starting service...");
        
        // Get a reference to the climate service
        serviceReference = context.getServiceReference(TemperatureHumidityService.class);
        
        if (serviceReference != null) {
            // Get the service
            TemperatureHumidityService climateService = context.getService(serviceReference);
            
            if (climateService != null) {
                // Create the climate manager
                climateManager = new CoolingHeatingManager(climateService);
                
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
        
        // Unget the service
        if (serviceReference != null) {
            context.ungetService(serviceReference);
        }
        
        System.out.println("[ClimateSubscriber] Service stopped");
    }
}