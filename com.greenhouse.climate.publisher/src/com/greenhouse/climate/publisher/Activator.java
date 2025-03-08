package com.greenhouse.climate.publisher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private ServiceRegistration<?> serviceRegistration;
    private TemperatureHumidityProducer producer;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[ClimatePublisher(Producer)] Starting service...");
        
        // Create the producer
        producer = new TemperatureHumidityProducer();
        
        // Register the service
        serviceRegistration = context.registerService(
            TemperatureHumidityService.class.getName(), 
            producer, 
            null);
        
        // Start generating climate data
        producer.start();
        
        System.out.println("[ClimatePublisher(Producer)] Service registered successfully");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[ClimatePublisher(Producer)] Stopping service...");
        
        // Stop the producer
        if (producer != null) {
            producer.stop();
        }
        
        // Unregister the service
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        
        System.out.println("[ClimatePublisher(Producer)] Service stopped");
    }
}