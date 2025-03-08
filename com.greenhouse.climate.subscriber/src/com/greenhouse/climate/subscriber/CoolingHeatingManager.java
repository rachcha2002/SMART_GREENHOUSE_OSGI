package com.greenhouse.climate.subscriber;

import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.greenhouse.climate.publisher.TemperatureHumidityService;
import com.greenhouse.report.IGreenhouseReporter;
import com.greenhouse.climate.publisher.ClimateData;

public class CoolingHeatingManager {
    private TemperatureHumidityService climateService;
    private Thread monitoringThread;
    private volatile boolean running = true;
    private IGreenhouseReporter reporter;

    
    // Store HVAC state for each zone
    private Map<String, ZoneHVACState> zoneHVACStates = new HashMap<>();
    
    // Define zone crop types (only needed in consumer)
    private static final Map<String, String> ZONE_CROP_MAPPING = new HashMap<>();
    
    // Define default climate control thresholds
    private static final double TEMP_BUFFER = 2.0; // Buffer beyond optimal range before HVAC activates
    private static final double HUMIDITY_BUFFER = 5.0; // Buffer beyond optimal range before HVAC activates
    
    private static final Map<String, double[]> CROP_OPTIMAL_TEMPS = new HashMap<>();
    private static final Map<String, double[]> CROP_OPTIMAL_HUMIDITY = new HashMap<>();
    
    // Initialize zone and crop data - only in consumer
    static {
        // Define which crop is in each zone
        ZONE_CROP_MAPPING.put("Zone-A", "Tomatoes");
        ZONE_CROP_MAPPING.put("Zone-B", "Cucumbers");
        ZONE_CROP_MAPPING.put("Zone-C", "Peppers");
        ZONE_CROP_MAPPING.put("Zone-D", "Lettuce");
        ZONE_CROP_MAPPING.put("Zone-E", "Herbs");
        
        // Define optimal temperature ranges for each crop [min, max]
        CROP_OPTIMAL_TEMPS.put("Tomatoes", new double[]{21.0, 27.0});
        CROP_OPTIMAL_TEMPS.put("Cucumbers", new double[]{23.0, 28.0});
        CROP_OPTIMAL_TEMPS.put("Peppers", new double[]{22.0, 26.0});
        CROP_OPTIMAL_TEMPS.put("Lettuce", new double[]{15.0, 22.0});
        CROP_OPTIMAL_TEMPS.put("Herbs", new double[]{18.0, 24.0});
        
        // Define optimal humidity ranges for each crop [min, max]
        CROP_OPTIMAL_HUMIDITY.put("Tomatoes", new double[]{65.0, 80.0});
        CROP_OPTIMAL_HUMIDITY.put("Cucumbers", new double[]{70.0, 85.0});
        CROP_OPTIMAL_HUMIDITY.put("Peppers", new double[]{65.0, 75.0});
        CROP_OPTIMAL_HUMIDITY.put("Lettuce", new double[]{60.0, 70.0});
        CROP_OPTIMAL_HUMIDITY.put("Herbs", new double[]{55.0, 70.0});
    }
    
    public CoolingHeatingManager(TemperatureHumidityService climateService, IGreenhouseReporter reporter) {
        this.climateService = climateService;
        this.reporter = reporter;
        
        // Initialize HVAC state for each zone
        String[] zones = climateService.getAvailableZones();
        for (String zoneId : zones) {
            // Only create HVAC state for zones that we know how to control (have crop mapping)
            if (ZONE_CROP_MAPPING.containsKey(zoneId)) {
                zoneHVACStates.put(zoneId, new ZoneHVACState());
                System.out.println("[CoolingHeatingManager] Initialized climate control for " + 
                                  zoneId + " with crop: " + ZONE_CROP_MAPPING.get(zoneId));
            }
        }
    }
    
    public void start() {
        System.out.println("[CoolingHeatingManager] Starting climate control system for all zones");
        System.out.println("[CoolingHeatingManager] Monitoring for sensor updates every 30 seconds");
        
        // Print the crop assignments and optimal ranges for each zone
        for (String zoneId : zoneHVACStates.keySet()) {
            String cropType = ZONE_CROP_MAPPING.get(zoneId);
            double[] tempRange = CROP_OPTIMAL_TEMPS.get(cropType);
            double[] humidityRange = CROP_OPTIMAL_HUMIDITY.get(cropType);
            
            System.out.println("[CoolingHeatingManager] Zone " + zoneId + " growing " + cropType + 
                              " (Optimal temp: " + tempRange[0] + "-" + tempRange[1] + "°C, " +
                              "humidity: " + humidityRange[0] + "-" + humidityRange[1] + "%)");
        }
        
        monitoringThread = new Thread(() -> {
            Map<String, Long> lastProcessedTime = new HashMap<>();
            
            // Initialize last processed time for all zones
            for (String zoneId : zoneHVACStates.keySet()) {
                lastProcessedTime.put(zoneId, 0L);
            }
            
            while (running) {
                try {
                    // Get climate data for all zones
                    Map<String, ClimateData> allZonesData = climateService.getAllZonesClimateData();
                    
                    if (allZonesData != null && !allZonesData.isEmpty()) {
                        boolean hasNewData = false;
                        Map<String, ClimateData> newData = new HashMap<>();
                        Map<String, String> requiredActions = new HashMap<>();
                        
                        // First, check if any zone has new data
                        for (String zoneId : allZonesData.keySet()) {
                            if (zoneHVACStates.containsKey(zoneId)) {
                                ClimateData zoneData = allZonesData.get(zoneId);
                                if (zoneData.getTimestamp() > lastProcessedTime.getOrDefault(zoneId, 0L)) {
                                    hasNewData = true;
                                    newData.put(zoneId, zoneData);
                                    lastProcessedTime.put(zoneId, zoneData.getTimestamp());
                                }
                            }
                        }
                        
                        // If we have new data, process it and generate a consolidated report
                        if (hasNewData) {
                            for (Map.Entry<String, ClimateData> entry : newData.entrySet()) {
                                String zoneId = entry.getKey();
                                ClimateData data = entry.getValue();
                                String action = processZoneClimate(zoneId, data);
                                if (action != null) {
                                    requiredActions.put(zoneId, action);
                                }
                            }
                            
                            // Generate a consolidated report for all zones
                            generateConsolidatedReport(newData, requiredActions);
                        }
                    }
                    
                    // Small sleep to prevent consuming too much CPU
                    Thread.sleep(100); // Just a short pause to prevent tight looping
                    
                } catch (Exception e) {
                    System.err.println("[CoolingHeatingManager] Error processing climate data: " + e.getMessage());
                    try {
                        Thread.sleep(1000); // Short wait before retrying
                    } catch (InterruptedException ie) {
                        running = false;
                        break;
                    }
                }
            }
        });
        
        monitoringThread.start();
    }

    private String processZoneClimate(String zoneId, ClimateData data) {
        if (data == null) return null;
        
        String cropType = ZONE_CROP_MAPPING.get(zoneId);
        if (cropType == null) {
            System.err.println("[CoolingHeatingManager] Unknown crop for zone: " + zoneId);
            return null;
        }
        
        // Get optimal ranges for this crop
        double[] tempRange = CROP_OPTIMAL_TEMPS.get(cropType);
        double[] humidityRange = CROP_OPTIMAL_HUMIDITY.get(cropType);
        
        // Set control thresholds with buffer
        double tempMin = tempRange[0];
        double tempMax = tempRange[1];
        double tempLow = tempMin - TEMP_BUFFER; // Activate heating below this
        double tempHigh = tempMax + TEMP_BUFFER; // Activate cooling above this
        
        double humidityMin = humidityRange[0];
        double humidityMax = humidityRange[1];
        double humidityLow = humidityMin - HUMIDITY_BUFFER; // Activate humidifier below this
        double humidityHigh = humidityMax + HUMIDITY_BUFFER; // Activate dehumidifier above this
        
        // Get current temperature and humidity
        double temperature = data.getTemperature();
        double humidity = data.getHumidity();
        
        // Get the HVAC state for this zone
        ZoneHVACState hvacState = zoneHVACStates.get(zoneId);
        
        // Check if conditions are outside optimal range
        boolean hasWarning = (temperature < tempMin || temperature > tempMax ||
                             humidity < humidityMin || humidity > humidityMax);
        
        // If there are warnings, process climate control actions
        if (hasWarning) {
            // Determine required actions
            return determineRequiredActions(zoneId, temperature, humidity,
                                          tempLow, tempMin, tempMax, tempHigh,
                                          humidityLow, humidityMin, humidityMax, humidityHigh,
                                          hvacState);
        }
        
        return null;
    }

    private void generateConsolidatedReport(Map<String, ClimateData> zoneData, Map<String, String> requiredActions) {
        // Format timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTime = dateFormat.format(new Date());
        
        StringBuilder report = new StringBuilder();
        report.append("=================================================\n");
        report.append("        Greenhouse Climate Monitoring Report     \n");
        report.append("=================================================\n");
        report.append("Timestamp      : ").append(formattedTime).append("\n");
        report.append("-------------------------------------------------\n");
        report.append("Zone | Crop      | Temp (°C) | Humidity (%) | Status \n");
        report.append("-------------------------------------------------\n");
        
        // Add data for each zone
        for (Map.Entry<String, ClimateData> entry : zoneData.entrySet()) {
            String zoneId = entry.getKey();
            ClimateData data = entry.getValue();
            String cropType = ZONE_CROP_MAPPING.get(zoneId);
            
            // Get optimal ranges for this crop
            double[] tempRange = CROP_OPTIMAL_TEMPS.get(cropType);
            double[] humidityRange = CROP_OPTIMAL_HUMIDITY.get(cropType);
            
            // Check if conditions are outside optimal range
            double temperature = data.getTemperature();
            double humidity = data.getHumidity();
            boolean tempWarning = (temperature < tempRange[0] || temperature > tempRange[1]);
            boolean humidityWarning = (humidity < humidityRange[0] || humidity > humidityRange[1]);
            
            String statusIcon = (tempWarning || humidityWarning) ? "⚠️" : "✅";
            String statusText;
            
            if (tempWarning && humidityWarning) {
                statusText = statusIcon + " TEMP & HUMIDITY ALERT";
            } else if (tempWarning) {
                if (temperature < tempRange[0]) {
                    statusText = statusIcon + " LOW TEMPERATURE ALERT";
                } else {
                    statusText = statusIcon + " HIGH TEMPERATURE ALERT";
                }
            } else if (humidityWarning) {
                if (humidity < humidityRange[0]) {
                    statusText = statusIcon + " LOW HUMIDITY ALERT";
                } else {
                    statusText = statusIcon + " HIGH HUMIDITY ALERT";
                }
            } else {
                statusText = statusIcon + " NORMAL - OPTIMAL CONDITIONS";
            }
            
            // Format each zone's data in a table row
            report.append(String.format("%-5s| %-10s| %-10s| %-13s| %s\n",
                                       zoneId,
                                       cropType,
                                       String.format("%.1f", temperature),
                                       String.format("%.1f", humidity),
                                       statusText));
        }
        
        report.append("=================================================\n");
        
        // Add required actions section if there are any
        if (!requiredActions.isEmpty()) {
            report.append("Required Actions:\n");
            report.append("-------------------------------------------------\n");
            
            for (Map.Entry<String, String> entry : requiredActions.entrySet()) {
                String zoneId = entry.getKey();
                String action = entry.getValue();
                String cropType = ZONE_CROP_MAPPING.get(zoneId);
                
                report.append(String.format("Zone %s (%s): %s\n", zoneId, cropType, action));
            }
            
            report.append("=================================================\n");
        }
        
        // Print the consolidated report
        System.out.println(report.toString());
    }

    private String determineRequiredActions(String zoneId, double temperature, double humidity,
                                          double tempLow, double tempMin, double tempMax, double tempHigh,
                                          double humidityLow, double humidityMin, double humidityMax, double humidityHigh,
                                          ZoneHVACState hvacState) {
        StringBuilder action = new StringBuilder();
        boolean actionTaken = false;
        
        // Handle temperature issues
        if (temperature < tempLow) {
            if (!hvacState.heatingActive) {
                action.append("Activating heating system");
                hvacState.heatingActive = true;
                actionTaken = true;
            }
            if (hvacState.coolingActive) {
                if (actionTaken) action.append(", ");
                action.append("Deactivating cooling system");
                hvacState.coolingActive = false;
                actionTaken = true;
            }
        }
        else if (temperature > tempHigh) {
            if (!hvacState.coolingActive) {
                action.append("Activating cooling system");
                hvacState.coolingActive = true;
                actionTaken = true;
            }
            if (hvacState.heatingActive) {
                if (actionTaken) action.append(", ");
                action.append("Deactivating heating system");
                hvacState.heatingActive = false;
                actionTaken = true;
            }
        }
        else if (temperature < tempMin && !hvacState.heatingActive) {
            action.append("Activating heating system");
            hvacState.heatingActive = true;
            actionTaken = true;
        }
        else if (temperature > tempMax && !hvacState.coolingActive) {
            action.append("Activating cooling system");
            hvacState.coolingActive = true;
            actionTaken = true;
        }
        
        // Handle humidity issues
        if (humidity < humidityLow) {
            if (actionTaken) action.append(", ");
            if (!hvacState.humidifierActive) {
                action.append("Activating humidifier");
                hvacState.humidifierActive = true;
                actionTaken = true;
            }
            if (hvacState.dehumidifierActive) {
                if (actionTaken) action.append(", ");
                action.append("Deactivating dehumidifier");
                hvacState.dehumidifierActive = false;
                actionTaken = true;
            }
        }
        else if (humidity > humidityHigh) {
            if (actionTaken) action.append(", ");
            if (!hvacState.dehumidifierActive) {
                action.append("Activating dehumidifier");
                hvacState.dehumidifierActive = true;
                actionTaken = true;
            }
            if (hvacState.humidifierActive) {
                if (actionTaken) action.append(", ");
                action.append("Deactivating humidifier");
                hvacState.humidifierActive = false;
                actionTaken = true;
            }
        }
        else if (humidity < humidityMin && !hvacState.humidifierActive) {
            if (actionTaken) action.append(", ");
            action.append("Activating humidifier");
            hvacState.humidifierActive = true;
            actionTaken = true;
        }
        else if (humidity > humidityMax && !hvacState.dehumidifierActive) {
            if (actionTaken) action.append(", ");
            action.append("Activating dehumidifier");
            hvacState.dehumidifierActive = true;
            actionTaken = true;
        }
        
        if (!actionTaken) {
            return null; // No action required
        }
        
        return action.toString();
    }

    private void controlZoneClimate(String zoneId, String zoneDisplay, 
                                  double temperature, double humidity,
                                  double tempLow, double tempMin, double tempMax, double tempHigh,
                                  double humidityLow, double humidityMin, double humidityMax, double humidityHigh,
                                  ZoneHVACState hvacState, StringBuilder logMessages) {
        // Control temperature
        boolean tempActionTaken = false;
        
        // Handle extreme temperatures first
        if (temperature < tempLow) {
            if (!hvacState.heatingActive) {
                logHVACAction(zoneDisplay, "🔥 Activating heating system", logMessages);
                hvacState.heatingActive = true;
                tempActionTaken = true;
            }
            if (hvacState.coolingActive) {
                logHVACAction(zoneDisplay, "⏸️ Deactivating cooling system", logMessages);
                hvacState.coolingActive = false;
                tempActionTaken = true;
            }
        }
        else if (temperature > tempHigh) {
            if (!hvacState.coolingActive) {
                logHVACAction(zoneDisplay, "🧊 Activating cooling system", logMessages);
                hvacState.coolingActive = true;
                tempActionTaken = true;
            }
            if (hvacState.heatingActive) {
                logHVACAction(zoneDisplay, "⏸️ Deactivating heating system", logMessages);
                hvacState.heatingActive = false;
                tempActionTaken = true;
            }
        }
        // Handle temperatures outside optimal range
        else if (temperature < tempMin && !hvacState.heatingActive) {
            logHVACAction(zoneDisplay, "🔥 Activating heating system", logMessages);
            hvacState.heatingActive = true;
            tempActionTaken = true;
        }
        else if (temperature > tempMax && !hvacState.coolingActive) {
            logHVACAction(zoneDisplay, "🧊 Activating cooling system", logMessages);
            hvacState.coolingActive = true;
            tempActionTaken = true;
        }
        
        // Control humidity
        boolean humidityActionTaken = false;
        
        // Handle extreme humidity levels first
        if (humidity < humidityLow) {
            if (!hvacState.humidifierActive) {
                logHVACAction(zoneDisplay, "💦 Activating humidifier", logMessages);
                hvacState.humidifierActive = true;
                humidityActionTaken = true;
            }
            if (hvacState.dehumidifierActive) {
                logHVACAction(zoneDisplay, "⏸️ Deactivating dehumidifier", logMessages);
                hvacState.dehumidifierActive = false;
                humidityActionTaken = true;
            }
        }
        else if (humidity > humidityHigh) {
            if (!hvacState.dehumidifierActive) {
                logHVACAction(zoneDisplay, "🌵 Activating dehumidifier", logMessages);
                hvacState.dehumidifierActive = true;
                humidityActionTaken = true;
            }
            if (hvacState.humidifierActive) {
                logHVACAction(zoneDisplay, "⏸️ Deactivating humidifier", logMessages);
                hvacState.humidifierActive = false;
                humidityActionTaken = true;
            }
        }
        // Handle humidity outside optimal range
        else if (humidity < humidityMin && !hvacState.humidifierActive) {
            logHVACAction(zoneDisplay, "💦 Activating humidifier", logMessages);
            hvacState.humidifierActive = true;
            humidityActionTaken = true;
        }
        else if (humidity > humidityMax && !hvacState.dehumidifierActive) {
            logHVACAction(zoneDisplay, "🌵 Activating dehumidifier", logMessages);
            hvacState.dehumidifierActive = true;
            humidityActionTaken = true;
        }
        
        // If no actions were taken, note this in the log
        if (!tempActionTaken && !humidityActionTaken) {
            logMessages.append("[HVAC-").append(zoneDisplay).append("] No immediate action required. Monitoring conditions.\n");
        }
    }

    private void logHVACAction(String zoneDisplay, String action, StringBuilder logMessages) {
        logMessages.append("[HVAC-").append(zoneDisplay).append("] ").append(action).append("\n");
    }
    
    private void controlZoneTemperature(String zoneId, String zoneDisplay, double temperature, 
                                      double tempLow, double tempMin, 
                                      double tempMax, double tempHigh,
                                      ZoneHVACState hvacState) {
        // Handle extreme temperatures first
        if (temperature < tempLow) {
            if (!hvacState.heatingActive) {
                activateHeating(zoneId, zoneDisplay, true);
                hvacState.heatingActive = true;
            }
            if (hvacState.coolingActive) {
                activateCooling(zoneId, zoneDisplay, false);
                hvacState.coolingActive = false;
            }
        }
        else if (temperature > tempHigh) {
            if (!hvacState.coolingActive) {
                activateCooling(zoneId, zoneDisplay, true);
                hvacState.coolingActive = true;
            }
            if (hvacState.heatingActive) {
                activateHeating(zoneId, zoneDisplay, false);
                hvacState.heatingActive = false;
            }
        }
        // Handle temperatures outside optimal range
        else if (temperature < tempMin && !hvacState.heatingActive) {
            activateHeating(zoneId, zoneDisplay, true);
            hvacState.heatingActive = true;
        }
        else if (temperature > tempMax && !hvacState.coolingActive) {
            activateCooling(zoneId, zoneDisplay, true);
            hvacState.coolingActive = true;
        }
        // Turn off systems when temperature is within optimal range
        else if (temperature >= tempMin && temperature <= tempMax) {
            if (hvacState.heatingActive) {
                activateHeating(zoneId, zoneDisplay, false);
                hvacState.heatingActive = false;
            }
            if (hvacState.coolingActive) {
                activateCooling(zoneId, zoneDisplay, false);
                hvacState.coolingActive = false;
            }
        }
    }
    
    private void controlZoneHumidity(String zoneId, String zoneDisplay, double humidity,
                                   double humidityLow, double humidityMin,
                                   double humidityMax, double humidityHigh,
                                   ZoneHVACState hvacState) {
        // Handle extreme humidity levels first
        if (humidity < humidityLow) {
            if (!hvacState.humidifierActive) {
                activateHumidifier(zoneId, zoneDisplay, true);
                hvacState.humidifierActive = true;
            }
            if (hvacState.dehumidifierActive) {
                activateDehumidifier(zoneId, zoneDisplay, false);
                hvacState.dehumidifierActive = false;
            }
        }
        else if (humidity > humidityHigh) {
            if (!hvacState.dehumidifierActive) {
                activateDehumidifier(zoneId, zoneDisplay, true);
                hvacState.dehumidifierActive = true;
            }
            if (hvacState.humidifierActive) {
                activateHumidifier(zoneId, zoneDisplay, false);
                hvacState.humidifierActive = false;
            }
        }
        // Handle humidity outside optimal range
        else if (humidity < humidityMin && !hvacState.humidifierActive) {
            activateHumidifier(zoneId, zoneDisplay, true);
            hvacState.humidifierActive = true;
        }
        else if (humidity > humidityMax && !hvacState.dehumidifierActive) {
            activateDehumidifier(zoneId, zoneDisplay, true);
            hvacState.dehumidifierActive = true;
        }
        // Turn off systems when humidity is within optimal range
        else if (humidity >= humidityMin && humidity <= humidityMax) {
            if (hvacState.humidifierActive) {
                activateHumidifier(zoneId, zoneDisplay, false);
                hvacState.humidifierActive = false;
            }
            if (hvacState.dehumidifierActive) {
                activateDehumidifier(zoneId, zoneDisplay, false);
                hvacState.dehumidifierActive = false;
            }
        }
    }
    
    private void activateCooling(String zoneId, String zoneDisplay, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneDisplay + "] 🧊 Activating cooling system");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Activated cooling system in " + zoneDisplay);
            }
        } else {
            System.out.println("[HVAC-" + zoneDisplay + "] ⏸️ Deactivating cooling system");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Deactivated cooling system in " + zoneDisplay);
            }
        }
    }

    private void activateHeating(String zoneId, String zoneDisplay, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneDisplay + "] 🔥 Activating heating system");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Activated heating system in " + zoneDisplay);
            }
        } else {
            System.out.println("[HVAC-" + zoneDisplay + "] ⏸️ Deactivating heating system");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Deactivated heating system in " + zoneDisplay);
            }
        }
    }

    private void activateHumidifier(String zoneId, String zoneDisplay, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneDisplay + "] 💦 Activating humidifier");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Activated humidifier in " + zoneDisplay);
            }
        } else {
            System.out.println("[HVAC-" + zoneDisplay + "] ⏸️ Deactivating humidifier");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Deactivated humidifier in " + zoneDisplay);
            }
        }
    }

    private void activateDehumidifier(String zoneId, String zoneDisplay, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneDisplay + "] 🌵 Activating dehumidifier");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Activated dehumidifier in " + zoneDisplay);
            }
        } else {
            System.out.println("[HVAC-" + zoneDisplay + "] ⏸️ Deactivating dehumidifier");
            if (reporter != null) {
                reporter.recordAction("Climate Control", "Deactivated dehumidifier in " + zoneDisplay);
            }
        }
    }
    
    public void stop() {
        running = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
        System.out.println("[CoolingHeatingManager] Climate control system stopped for all zones");
    }
    
    // Inner class to track HVAC state for each zone
    private static class ZoneHVACState {
        boolean coolingActive = false;
        boolean heatingActive = false;
        boolean humidifierActive = false;
        boolean dehumidifierActive = false;
    }
}