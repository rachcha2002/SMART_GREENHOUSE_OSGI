package com.greenhouse.report;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    private GreenhouseActionReporter reporter;
    private Thread reporterThread;
    private ServiceRegistration<?> serviceRegistration;
    
    @Override
    public void start(BundleContext context) {
        System.out.println("[ReportActivator] Starting Greenhouse Action Reporter...");
        
        // Create the reporter
        reporter = new GreenhouseActionReporter();
        
        // Register it as a service with the interface
        serviceRegistration = context.registerService(
            IGreenhouseReporter.class.getName(), reporter, null);
        
        // Run it in a separate thread
        reporterThread = new Thread(reporter);
        reporterThread.start();
        
        System.out.println("[ReportActivator] Greenhouse Action Reporter started and monitoring for 60 seconds.");
    }
    
    @Override
    public void stop(BundleContext context) {
        System.out.println("[ReportActivator] Stopping Greenhouse Action Reporter...");
        
        if (reporter != null) {
            reporter.shutdown();
        }
        
        if (reporterThread != null) {
            reporterThread.interrupt();
            try {
                reporterThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        
        System.out.println("[ReportActivator] Greenhouse Action Reporter stopped.");
    }
}