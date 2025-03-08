package com.greenhouse.climate.subscriber;

import com.greenhouse.climate.publisher.TemperatureHumidityService;
import com.greenhouse.climate.publisher.ClimateData;

public class CoolingHeatingManager {
    private TemperatureHumidityService climateService;
    private Thread monitoringThread;
    private volatile boolean running = true;
    
    // Define climate control thresholds
    private static final double TEMP_TOO_HIGH = 27.0; // °C
    private static final double TEMP_HIGH = 25.0; // °C
    private static final double TEMP_LOW = 22.0; // °C
    private static final double TEMP_TOO_LOW = 20.0; // °C
    
    private static final double HUMIDITY_TOO_HIGH = 85.0; // %
    private static final double HUMIDITY_HIGH = 80.0; // %
    private static final double HUMIDITY_LOW = 60.0; // %
    private static final double HUMIDITY_TOO_LOW = 55.0; // %
    
    // Track current HVAC state
    private boolean coolingActive = false;
    private boolean heatingActive = false;
    private boolean humidifierActive = false;
    private boolean dehumidifierActive = false;
    
    public CoolingHeatingManager(TemperatureHumidityService climateService) {
        this.climateService = climateService;
    }
    
    public void start() {
        System.out.println("[CoolingHeatingManager(Consumer)] Starting climate control system");
        
        monitoringThread = new Thread(() -> {
            while (running) {
                try {
                    // Get the latest climate data
                    ClimateData data = climateService.getClimateData();
                    if (data != null) {
                        // Process the climate data and adjust HVAC systems
                        controlTemperature(data.getTemperature());
                        controlHumidity(data.getHumidity());
                    } else {
                        System.out.println("[CoolingHeatingManager(Consumer)] ⚠️ Warning: No climate data available");
                    }
                    
                    // Wait before checking again
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (Exception e) {
                    System.err.println("[CoolingHeatingManager(Consumer)] Error processing climate data: " + e.getMessage());
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
    
    private void controlTemperature(double temperature) {
        // Handle extreme temperatures first
        if (temperature > TEMP_TOO_HIGH) {
            if (!coolingActive) {
                activateCooling(true);
            }
            if (heatingActive) {
                activateHeating(false);
            }
        }
        else if (temperature < TEMP_TOO_LOW) {
            if (!heatingActive) {
                activateHeating(true);
            }
            if (coolingActive) {
                activateCooling(false);
            }
        }
        // Handle more moderate temperature imbalances
        else if (temperature > TEMP_HIGH && !coolingActive) {
            activateCooling(true);
        }
        else if (temperature < TEMP_LOW && !heatingActive) {
            activateHeating(true);
        }
        // Turn off systems when temperature is back to normal range
        else if (temperature <= TEMP_HIGH && temperature >= TEMP_LOW) {
            if (coolingActive) {
                activateCooling(false);
            }
            if (heatingActive) {
                activateHeating(false);
            }
        }
    }
    
    private void controlHumidity(double humidity) {
        // Handle extreme humidity levels first
        if (humidity > HUMIDITY_TOO_HIGH) {
            if (!dehumidifierActive) {
                activateDehumidifier(true);
            }
            if (humidifierActive) {
                activateHumidifier(false);
            }
        }
        else if (humidity < HUMIDITY_TOO_LOW) {
            if (!humidifierActive) {
                activateHumidifier(true);
            }
            if (dehumidifierActive) {
                activateDehumidifier(false);
            }
        }
        // Handle more moderate humidity imbalances
        else if (humidity > HUMIDITY_HIGH && !dehumidifierActive) {
            activateDehumidifier(true);
        }
        else if (humidity < HUMIDITY_LOW && !humidifierActive) {
            activateHumidifier(true);
        }
        // Turn off systems when humidity is back to normal range
        else if (humidity <= HUMIDITY_HIGH && humidity >= HUMIDITY_LOW) {
            if (dehumidifierActive) {
                activateDehumidifier(false);
            }
            if (humidifierActive) {
                activateHumidifier(false);
            }
        }
    }
    
    private void activateCooling(boolean activate) {
        coolingActive = activate;
        if (activate) {
            System.out.println("[CoolingHeatingManager(Consumer)] 🧊 Activating cooling system");
        } else {
            System.out.println("[CoolingHeatingManager(Consumer)] ⏸️ Deactivating cooling system");
        }
    }
    
    private void activateHeating(boolean activate) {
        heatingActive = activate;
        if (activate) {
            System.out.println("[CoolingHeatingManager(Consumer)] 🔥 Activating heating system");
        } else {
            System.out.println("[CoolingHeatingManager(Consumer)] ⏸️ Deactivating heating system");
        }
    }
    
    private void activateHumidifier(boolean activate) {
        humidifierActive = activate;
        if (activate) {
            System.out.println("[CoolingHeatingManager(Consumer)] 💦 Activating humidifier");
        } else {
            System.out.println("[CoolingHeatingManager(Consumer)] ⏸️ Deactivating humidifier");
        }
    }
    
    private void activateDehumidifier(boolean activate) {
        dehumidifierActive = activate;
        if (activate) {
            System.out.println("[CoolingHeatingManager(Consumer)] 🌵 Activating dehumidifier");
        } else {
            System.out.println("[CoolingHeatingManager(Consumer)] ⏸️ Deactivating dehumidifier");
        }
    }
    
    public void stop() {
        running = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
        System.out.println("[CoolingHeatingManager(Consumer)] Climate control system stopped");
    }
}