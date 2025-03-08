package com.greenhouse.pest.servicesubscriber;

import com.greenhouse.pest.servicepublisher.PestServicePublish;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Activator implements BundleActivator {

    private ServiceReference<PestServicePublish> pestRef;
    private PestServicePublish pestService;
    private ExecutorService executorService;
    private volatile boolean running = true;
    private BundleContext context;

    @Override
    public void start(BundleContext context) {
        this.context = context;
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
        System.out.println("[PestControlSystem] Stopping...");
    }
}