package com.greenhouse.pest.servicesubscriber;
import com.greenhouse.pest.servicepublisher.PestServicePublish;
import com.greenhouse.report.IGreenhouseReporter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator {
    private ServiceReference<PestServicePublish> pestRef;
    private ServiceReference<IGreenhouseReporter> reporterRef;
    private PestServicePublish pestService;
    private IGreenhouseReporter reporter;
    private ExecutorService executorService;
    private volatile boolean running = true;
    private BundleContext context;
    
    @Override
    public void start(BundleContext context) {
        this.context = context;
        
        // First, try to get the reporter service
        reporterRef = context.getServiceReference(IGreenhouseReporter.class);
        if (reporterRef != null) {
            reporter = context.getService(reporterRef);
            System.out.println("[PestControlSystem] Connected to greenhouse reporter.");
        } else {
            System.out.println("[PestControlSystem] Greenhouse reporter not available.");
        }
        
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::listenForPestData);
    }
    
    private void listenForPestData() {
        while (running) {
            try {
                // Try to connect to the Pest Detection Service
                pestRef = context.getServiceReference(PestServicePublish.class);
                if (pestRef == null) {
                    System.err.println("[PestControlSystem] ERROR: Lost connection to Pest Detection Service. Retrying...");
                    Thread.sleep(5000);  // Retry every 5 seconds
                    continue;
                }
                pestService = context.getService(pestRef);
                if (pestService == null) {
                    System.err.println("[PestControlSystem] ERROR: Pest Detection Service unavailable. Retrying...");
                    Thread.sleep(5000);  // Retry every 5 seconds
                    continue;
                }
                
                // Fetch the latest pest detection result from the producer
                String pestStatus = pestService.detectPests();
                System.out.println(pestStatus);
                
                // Record monitoring action
                if (reporter != null) {
                    reporter.recordAction("Pest Control", "Monitored pest levels: " + pestStatus);
                }
                
                activateControl(pestStatus);
                
                // Wait for 30 seconds before fetching the next update
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                System.err.println("[PestControlSystem] ERROR: Interrupted while sleeping.");
                break;
            }
        }
    }
    
    private void activateControl(String pestStatus) {
        if (pestStatus.contains("Detected") || pestStatus.matches(".*\\d+.*")) {
            System.out.println("🛑 Deploying organic pesticides...");
            
            // Report pest control action
            if (reporter != null) {
                reporter.recordAction("Pest Control", "Deployed organic pesticides due to pest detection");
            }
        }
    }
    
    @Override
    public void stop(BundleContext context) {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
        }
        
        if (pestRef != null) {
            context.ungetService(pestRef);
        }
        
        if (reporterRef != null) {
            context.ungetService(reporterRef);
        }
        
        System.out.println("[PestControlSystem] Stopping...");
    }
}