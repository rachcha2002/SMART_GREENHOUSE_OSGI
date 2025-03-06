package com.greenhouse.soilmoisture;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import com.soilmoisture.api.ISoilMoistureService;
import java.util.Random;

public class Activator implements BundleActivator, ISoilMoistureService {

    private ServiceRegistration<ISoilMoistureService> registration;
    private Random random;

    @Override
    public void start(BundleContext context) throws Exception {
        random = new Random();

        // Register this class as a service providing ISoilMoistureService
        registration = context.registerService(ISoilMoistureService.class, this, null);

        System.out.println("[SoilMoistureProducer] Bundle Started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Unregister service
        if (registration != null) {
            registration.unregister();
        }

        System.out.println("[SoilMoistureProducer] Bundle Stopped");
    }

    @Override
    public double getSoilMoisture() {
        // Generate dummy moisture data between 0 and 100
        double moisture = 10 + (90 * random.nextDouble());
        System.out.printf("[SoilMoistureProducer] Generated moisture level: %.2f%%%n", moisture);
        return moisture;
    }
}