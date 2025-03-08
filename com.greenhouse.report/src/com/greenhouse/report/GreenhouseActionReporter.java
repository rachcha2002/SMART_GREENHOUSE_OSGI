package com.greenhouse.report;

import java.util.*;
import java.util.concurrent.*;

/**
 * Implementation of the greenhouse reporter
 */
public class GreenhouseActionReporter implements IGreenhouseReporter, Runnable {
    
    // Map to store actions by service type
    private final Map<String, List<String>> serviceActions = new ConcurrentHashMap<>();
    
    // Executors for scheduling
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Flag to control monitoring
    private volatile boolean isRunning = false;
    
    // Semaphore to track when the monitoring period is complete
    private final Semaphore reportReady = new Semaphore(0);
    
    /**
     * Initializes the action reporter
     */
    public GreenhouseActionReporter() {
        // Initialize action lists for each service
        serviceActions.put("Climate Control", new CopyOnWriteArrayList<>());
        serviceActions.put("Light System", new CopyOnWriteArrayList<>());
        serviceActions.put("Irrigation System", new CopyOnWriteArrayList<>());
        serviceActions.put("Pest Control", new CopyOnWriteArrayList<>());
    }
    
    @Override
    public void recordAction(String serviceType, String action) {
        if (isRunning) {
            List<String> actions = serviceActions.get(serviceType);
            if (actions != null) {
                actions.add(action + " [" + new Date() + "]");
                System.out.println("[Reporter] Recorded: " + serviceType + " - " + action);
            }
        }
    }
    
    @Override
    public void startMonitoring(int durationSeconds) {
        // Clear previous data
        for (List<String> actions : serviceActions.values()) {
            actions.clear();
        }
        
        isRunning = true;
        System.out.println("\n[Reporter] ========= Started monitoring greenhouse systems for " + 
                durationSeconds + " seconds =========");
        
        // Schedule the stop after the specified duration
        scheduler.schedule(() -> {
            isRunning = false;
            reportReady.release(); // Signal that monitoring is complete
            System.out.println("[Reporter] ========= Monitoring period ended =========\n");
        }, durationSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public String generateReport() {
        try {
            // Wait for monitoring to complete
            boolean success = reportReady.tryAcquire(5, TimeUnit.SECONDS);
            if (!success) {
                System.out.println("[Reporter] Warning: Report generated before monitoring completed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        StringBuilder report = new StringBuilder();
        
        report.append("\n=====================================================\n");
        report.append("       GREENHOUSE SYSTEM ACTIVITY REPORT            \n");
        report.append("=====================================================\n");
        report.append("Generated: ").append(new Date()).append("\n\n");
        
        // Collect overall statistics
        int totalActions = 0;
        for (List<String> actions : serviceActions.values()) {
            totalActions += actions.size();
        }
        
        report.append("SUMMARY:\n");
        report.append("Total actions recorded: ").append(totalActions).append("\n");
        
        // Add per-service summary
        for (Map.Entry<String, List<String>> entry : serviceActions.entrySet()) {
            String service = entry.getKey();
            List<String> actions = entry.getValue();
            
            report.append("- ").append(service).append(": ").append(actions.size()).append(" actions\n");
        }
        
        report.append("\nDETAILED ACTIONS BY SERVICE:\n");
        
        // Add detailed actions for each service
        for (Map.Entry<String, List<String>> entry : serviceActions.entrySet()) {
            String service = entry.getKey();
            List<String> actions = entry.getValue();
            
            report.append("\n").append(service).append(":\n");
            report.append("------------------------\n");
            
            if (actions.isEmpty()) {
                report.append("No actions recorded during monitoring period.\n");
            } else {
                // Count occurrences of similar actions
                Map<String, Integer> actionCounts = new HashMap<>();
                for (String action : actions) {
                    // Extract the action without the timestamp
                    String baseAction = action.substring(0, action.lastIndexOf(" ["));
                    actionCounts.put(baseAction, actionCounts.getOrDefault(baseAction, 0) + 1);
                }
                
                // List action types with counts
                for (Map.Entry<String, Integer> actionEntry : actionCounts.entrySet()) {
                    report.append("- ").append(actionEntry.getKey())
                          .append(": ").append(actionEntry.getValue())
                          .append(" times\n");
                }
                
                // Show the full action log
                report.append("\nAction log:\n");
                for (int i = 0; i < actions.size(); i++) {
                    report.append(i+1).append(". ").append(actions.get(i)).append("\n");
                }
            }
        }
        
        report.append("\n=====================================================\n");
        report.append("                 END OF REPORT                      \n");
        report.append("=====================================================\n");
        
        return report.toString();
    }
    
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Start a new monitoring period
                startMonitoring(60);
                
                // Wait for monitoring to complete
                reportReady.acquire();
                
                // Print the report
                System.out.println(generateReport());
                
                // Small pause between cycles (optional)
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Reporter] Reporting thread interrupted, stopping");
                break;
            }
        }
    }
    
    /**
     * Shuts down the reporter's resources
     */
    public void shutdown() {
        isRunning = false;
        reportReady.release(); // Release any waiting threads
        scheduler.shutdownNow();
    }
}