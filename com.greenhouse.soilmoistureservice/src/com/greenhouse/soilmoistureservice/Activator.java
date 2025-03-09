package com.greenhouse.soilmoistureservice;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        SoilMoistureProducer producer = new SoilMoistureProducer();
        // Register the producer class as a service using its fully qualified class name
        registration = context.registerService(SoilMoistureProducer.class.getName(), producer, null);
        System.out.println("[SoilMoistureProducer] Service Registered.");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // No explicit unregister needed; OSGi will unregister the service on bundle stop.
        System.out.println("[SoilMoistureProducer] Bundle Stopped.");
    }
}
