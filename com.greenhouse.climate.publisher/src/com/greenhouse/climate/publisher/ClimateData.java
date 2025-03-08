package com.greenhouse.climate.publisher;

public class ClimateData {
    private String zoneId;
    private double temperature;
    private double humidity;
    private long timestamp;
    
    public ClimateData(String zoneId, double temperature, double humidity) {
        this.zoneId = zoneId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getZoneId() {
        return zoneId;
    }
    
    public double getTemperature() {
        return temperature;
    }
    
    public double getHumidity() {
        return humidity;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("%s - Temperature: %.1f°C, Humidity: %.1f%%", 
                            zoneId, temperature, humidity);
    }
}