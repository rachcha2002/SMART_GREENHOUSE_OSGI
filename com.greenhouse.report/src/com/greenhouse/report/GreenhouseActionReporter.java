package com.greenhouse.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementation of the greenhouse reporter
 */
public class GreenhouseActionReporter implements IGreenhouseReporter, Runnable {
    
    private final Map<String, List<String>> serviceActions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean isRunning = false;
    private final Semaphore reportReady = new Semaphore(0);
    
    public GreenhouseActionReporter() {
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
        for (List<String> actions : serviceActions.values()) {
            actions.clear();
        }
        
        isRunning = true;
        System.out.println("\n[Reporter] ========= Started monitoring greenhouse systems for " + 
                durationSeconds + " seconds =========");
        
        scheduler.schedule(() -> {
            isRunning = false;
            reportReady.release();
            System.out.println("[Reporter] ========= Monitoring period ended =========\n");
        }, durationSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public String generateReport() {
        try {
            boolean success = reportReady.tryAcquire(5, TimeUnit.SECONDS);
            if (!success) {
                System.out.println("[Reporter] Warning: Report generated before monitoring completed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        StringBuilder report = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        
        report.append("\n=====================================================\n");
        report.append("       GREENHOUSE SYSTEM ACTIVITY REPORT            \n");
        report.append("=====================================================\n");
        report.append("Generated: ").append(new Date()).append("\n\n");
        
        int totalActions = serviceActions.values().stream().mapToInt(List::size).sum();
        report.append("SUMMARY:\n");
        report.append("Total actions recorded: ").append(totalActions).append("\n");
        
        for (Map.Entry<String, List<String>> entry : serviceActions.entrySet()) {
            report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" actions\n");
        }
        
        report.append("\nDETAILED ACTIONS BY SERVICE:\n");
        for (Map.Entry<String, List<String>> entry : serviceActions.entrySet()) {
            report.append("\n").append(entry.getKey()).append(":\n");
            report.append("------------------------\n");
            
            if (entry.getValue().isEmpty()) {
                report.append("No actions recorded during monitoring period.\n");
            } else {
                for (int i = 0; i < entry.getValue().size(); i++) {
                    report.append(i + 1).append(". ").append(entry.getValue().get(i)).append("\n");
                }
            }
        }
        
        report.append("\n=====================================================\n");
        report.append("                 END OF REPORT                      \n");
        report.append("=====================================================\n");
        
        String reportContent = report.toString();
        saveReportToFile(reportContent, "D:/GreenhouseReports/Greenhouse_Report_" + timestamp + ".txt");
        
        return reportContent;
    }
    
    private void saveReportToFile(String content, String filename) {
        Path filePath = Path.of(filename);
        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            System.out.println("[Reporter] Report saved as: " + filename);
        } catch (IOException e) {
            System.err.println("[Reporter] Error saving report: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                startMonitoring(60);
                reportReady.acquire();
                System.out.println(generateReport());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Reporter] Reporting thread interrupted, stopping");
                break;
            }
        }
    }
    
    public void shutdown() {
        isRunning = false;
        reportReady.release();
        scheduler.shutdownNow();
    }
}