package com.greenhouse.climate.subscriber;

import java.util.HashMap;
import java.util.Map;

import com.greenhouse.climate.publisher.TemperatureHumidityService;
import com.greenhouse.climate.publisher.ClimateData;

public class CoolingHeatingManager {
    private TemperatureHumidityService climateService;
    private Thread monitoringThread;
    private volatile boolean running = true;
    
    // Store HVAC state for each zone
    private Map<String, ZoneHVACState> zoneHVACStates = new HashMap<>();
    
    // Define default climate control thresholds
    // Each zone will adjust these based on crop needs
    private static final double TEMP_BUFFER = 2.0; // Buffer beyond optimal range before HVAC activates
    private static final double HUMIDITY_BUFFER = 5.0; // Buffer beyond optimal range before HVAC activates
    
    private static final Map<String, double[]> CROP_OPTIMAL_TEMPS = new HashMap<>();
    private static final Map<String, double[]> CROP_OPTIMAL_HUMIDITY = new HashMap<>();
    private static final Map<String, String> GREENHOUSE_ZONES = new HashMap<>();
    
    // Initialize zone and crop data - must match producer data
    static {
        // Define zones and crops
        GREENHOUSE_ZONES.put("Zone-A", "Tomatoes");
        GREENHOUSE_ZONES.put("Zone-B", "Cucumbers");
        GREENHOUSE_ZONES.put("Zone-C", "Peppers");
        GREENHOUSE_ZONES.put("Zone-D", "Lettuce");
        GREENHOUSE_ZONES.put("Zone-E", "Herbs");
        
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
    
    public CoolingHeatingManager(TemperatureHumidityService climateService) {
        this.climateService = climateService;
        
        // Initialize HVAC state for each zone
        for (String zoneId : GREENHOUSE_ZONES.keySet()) {
            zoneHVACStates.put(zoneId, new ZoneHVACState());
        }
    }
    
    public void start() {
        System.out.println("[CoolingHeatingManager] Starting climate control system for all zones");
        
        monitoringThread = new Thread(() -> {
            while (running) {
                try {
                    // Get climate data for all zones
                    Map<String, ClimateData> allZonesData = climateService.getAllZonesClimateData();
                    
                    if (allZonesData != null && !allZonesData.isEmpty()) {
                        // Process climate data for each zone
                        for (String zoneId : allZonesData.keySet()) {
                            ClimateData zoneData = allZonesData.get(zoneId);
                            processZoneClimate(zoneId, zoneData);
                        }
                    } else {
                        System.out.println("[CoolingHeatingManager] ⚠️ Warning: No climate data available");
                    }
                    
                    // Wait before checking again
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (Exception e) {
                    System.err.println("[CoolingHeatingManager] Error processing climate data: " + e.getMessage());
                    try {
                        Thread.sleep(5000); // Wait before retrying
                    } catch (InterruptedException ie) {
                        running = false;
                        break;
                    }
                }
            }
        });
        
        monitoringThread.start();
    }
    
    private void processZoneClimate(String zoneId, ClimateData data) {
        if (data == null) return;
        
        String cropType = GREENHOUSE_ZONES.get(zoneId);
        if (cropType == null) {
            System.err.println("[CoolingHeatingManager] Unknown zone: " + zoneId);
            return;
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
        
        // Control temperature for this zone
        controlZoneTemperature(zoneId, temperature, tempLow, tempMin, tempMax, tempHigh, hvacState);
        
        // Control humidity for this zone
        controlZoneHumidity(zoneId, humidity, humidityLow, humidityMin, humidityMax, humidityHigh, hvacState);
    }
    
    private void controlZoneTemperature(String zoneId, double temperature, 
                                      double tempLow, double tempMin, 
                                      double tempMax, double tempHigh,
                                      ZoneHVACState hvacState) {
        // Handle extreme temperatures first
        if (temperature < tempLow) {
            if (!hvacState.heatingActive) {
                activateHeating(zoneId, true);
                hvacState.heatingActive = true;
            }
            if (hvacState.coolingActive) {
                activateCooling(zoneId, false);
                hvacState.coolingActive = false;
            }
        }
        else if (temperature > tempHigh) {
            if (!hvacState.coolingActive) {
                activateCooling(zoneId, true);
                hvacState.coolingActive = true;
            }
            if (hvacState.heatingActive) {
                activateHeating(zoneId, false);
                hvacState.heatingActive = false;
            }
        }
        // Handle temperatures outside optimal range
        else if (temperature < tempMin && !hvacState.heatingActive) {
            activateHeating(zoneId, true);
            hvacState.heatingActive = true;
        }
        else if (temperature > tempMax && !hvacState.coolingActive) {
            activateCooling(zoneId, true);
            hvacState.coolingActive = true;
        }
        // Turn off systems when temperature is within optimal range
        else if (temperature >= tempMin && temperature <= tempMax) {
            if (hvacState.heatingActive) {
                activateHeating(zoneId, false);
                hvacState.heatingActive = false;
            }
            if (hvacState.coolingActive) {
                activateCooling(zoneId, false);
                hvacState.coolingActive = false;
            }
        }
    }
    
    private void controlZoneHumidity(String zoneId, double humidity,
                                   double humidityLow, double humidityMin,
                                   double humidityMax, double humidityHigh,
                                   ZoneHVACState hvacState) {
        // Handle extreme humidity levels first
        if (humidity < humidityLow) {
            if (!hvacState.humidifierActive) {
                activateHumidifier(zoneId, true);
                hvacState.humidifierActive = true;
            }
            if (hvacState.dehumidifierActive) {
                activateDehumidifier(zoneId, false);
                hvacState.dehumidifierActive = false;
            }
        }
        else if (humidity > humidityHigh) {
            if (!hvacState.dehumidifierActive) {
                activateDehumidifier(zoneId, true);
                hvacState.dehumidifierActive = true;
            }
            if (hvacState.humidifierActive) {
                activateHumidifier(zoneId, false);
                hvacState.humidifierActive = false;
            }
        }
        // Handle humidity outside optimal range
        else if (humidity < humidityMin && !hvacState.humidifierActive) {
            activateHumidifier(zoneId, true);
            hvacState.humidifierActive = true;
        }
        else if (humidity > humidityMax && !hvacState.dehumidifierActive) {
            activateDehumidifier(zoneId, true);
            hvacState.dehumidifierActive = true;
        }
        // Turn off systems when humidity is within optimal range
        else if (humidity >= humidityMin && humidity <= humidityMax) {
            if (hvacState.humidifierActive) {
                activateHumidifier(zoneId, false);
                hvacState.humidifierActive = false;
            }
            if (hvacState.dehumidifierActive) {
                activateDehumidifier(zoneId, false);
                hvacState.dehumidifierActive = false;
            }
        }
    }
    
    private void activateCooling(String zoneId, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneId + "] 🧊 Activating cooling system");
        } else {
            System.out.println("[HVAC-" + zoneId + "] ⏸️ Deactivating cooling system");
        }
    }
    
    private void activateHeating(String zoneId, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneId + "] 🔥 Activating heating system");
        } else {
            System.out.println("[HVAC-" + zoneId + "] ⏸️ Deactivating heating system");
        }
    }
    
    private void activateHumidifier(String zoneId, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneId + "] 💦 Activating humidifier");
        } else {
            System.out.println("[HVAC-" + zoneId + "] ⏸️ Deactivating humidifier");
        }
    }
    
    private void activateDehumidifier(String zoneId, boolean activate) {
        if (activate) {
            System.out.println("[HVAC-" + zoneId + "] 🌵 Activating dehumidifier");
        } else {
            System.out.println("[HVAC-" + zoneId + "] ⏸️ Deactivating dehumidifier");
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