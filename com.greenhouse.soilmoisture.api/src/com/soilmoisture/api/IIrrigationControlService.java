package com.soilmoisture.api;

public interface IIrrigationControlService {
	  /**
     * Decide and print irrigation commands based on the given moisture level.
     */
    void controlIrrigation(double moistureLevel);
}