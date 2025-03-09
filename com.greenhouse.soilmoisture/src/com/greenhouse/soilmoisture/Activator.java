package com.greenhouse.soilmoisture;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.soilmoisture.api.ISoilMoistureService;

public class Activator implements BundleActivator {
    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) {
        SoilMoistureProducer producer = new SoilMoistureProducer();
        registration = context.registerService(ISoilMoistureService.class, producer, null);
        System.out.println("[SoilMoistureProducer] Service Registered.");
    }

    @Override
    public void stop(BundleContext context) {
//        registration.unregister();
        System.out.println("[SoilMoistureProducer] Service Unregistered.");
    }
}
