package com.greenhouse.report;

/**
 * Interface for greenhouse system reporting
 */
public interface IGreenhouseReporter {
    /**
     * Records an action taken by a service
     * @param serviceType The type of service (e.g., "Climate Control", "Light System")
     * @param action Description of the action taken
     */
    void recordAction(String serviceType, String action);
    
    /**
     * Start monitoring for a specified duration
     * @param durationSeconds Duration in seconds
     */
    void startMonitoring(int durationSeconds);
    
    /**
     * Generate a report of all actions during the monitoring period
     * @return Formatted report as a string
     */
    String generateReport();
}