package com.soilmoisture.api;

import java.util.Map;

public interface ISoilMoistureService {
    Map<String, Double> getSoilMoistureLevels();  // Returns moisture levels for all zones
}
