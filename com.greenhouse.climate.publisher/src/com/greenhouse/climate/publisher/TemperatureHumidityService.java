package com.greenhouse.climate.publisher;

import java.util.Map;

public interface TemperatureHumidityService {
    // Get climate data for all zones
    Map<String, ClimateData> getAllZonesClimateData();
    
    // Get climate data for a specific zone
    ClimateData getZoneClimateData(String zoneId);
    
    // Get all available zone IDs
    String[] getAvailableZones();
}