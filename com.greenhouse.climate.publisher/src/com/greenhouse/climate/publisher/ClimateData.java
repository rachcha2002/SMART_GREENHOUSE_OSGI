package com.greenhouse.climate.publisher;

public class ClimateData {
    private double temperature;
    private double humidity;
    private long timestamp;
    
    public ClimateData(double temperature, double humidity) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = System.currentTimeMillis();
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
        return String.format("Temperature: %.1f°C, Humidity: %.1f%%", temperature, humidity);
    }
}