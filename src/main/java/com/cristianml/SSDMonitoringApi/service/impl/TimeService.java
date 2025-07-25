package com.cristianml.SSDMonitoringApi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Service implementation for external time synchronization
// Provides reliable current time retrieval with fallback to system clock
// Ensures consistent timestamp generation across distributed monitoring instances
@Service
public class TimeService {

    private static final String TIME_API_URL = "https://timeapi.io/api/Time/current/zone?timeZone=America/La_Paz";
    private static final Logger logger = LoggerFactory.getLogger(TimeService.class);

    // Retrieves current date and time from external API with system fallback
    // Primary source is timeapi.io with La Paz timezone configuration
    public LocalDateTime getCurrentDateTime() {
        logger.debug("Fetching current date and time from TimeAPI");
        try {
            RestTemplate restTemplate = new RestTemplate();
            TimeApiResponse response = restTemplate.getForObject(TIME_API_URL, TimeApiResponse.class);
            if (response != null && response.getDateTime() != null) {
                // Parse ISO 8601 formatted datetime string from API response
                LocalDateTime apiDateTime = LocalDateTime.parse(response.getDateTime(), DateTimeFormatter.ISO_DATE_TIME);
                logger.info("Successfully fetched date and time from TimeAPI: {}", apiDateTime);
                return apiDateTime;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch time from TimeAPI, using system time instead", e);
        }
        // Fallback to local system time when external API is unavailable
        LocalDateTime systemDateTime = LocalDateTime.now();
        logger.info("Using system date and time: {}", systemDateTime);
        return systemDateTime;
    }

    // Verifies availability of external time API service
    // Used to determine if timestamps should be considered authoritative
    public boolean isApiDateAvailable() {
        logger.debug("Checking if API date is available");
        try {
            RestTemplate restTemplate = new RestTemplate();
            TimeApiResponse response = restTemplate.getForObject(TIME_API_URL, TimeApiResponse.class);
            boolean isAvailable = response != null && response.getDateTime() != null;
            logger.info("API date availability: {}", isAvailable);
            return isAvailable;
        } catch (Exception e) {
            logger.warn("Failed to fetch time from TimeAPI", e);
            return false;
        }
    }

    // Internal response mapping class for time API JSON structure
    private static class TimeApiResponse {
        private String dateTime;

        public String getDateTime() {
            return dateTime;
        }

        public void setDateTime(String dateTime) {
            this.dateTime = dateTime;
        }
    }
}