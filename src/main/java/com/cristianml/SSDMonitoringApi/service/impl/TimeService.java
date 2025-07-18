package com.cristianml.SSDMonitoringApi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class provides services for retrieving the current date and time from an external API.
 * It falls back to the system time if the API is unavailable.
 */
@Service
public class TimeService {

    private static final String TIME_API_URL = "https://timeapi.io/api/Time/current/zone?timeZone=America/La_Paz";
    private static final Logger logger = LoggerFactory.getLogger(TimeService.class);

    /**
     * Gets the current date and time from the API. If the API is unavailable, falls back to the system time.
     *
     * @return the current date and time.
     */
    public LocalDateTime getCurrentDateTime() {
        logger.debug("Fetching current date and time from TimeAPI");
        try {
            RestTemplate restTemplate = new RestTemplate();
            TimeApiResponse response = restTemplate.getForObject(TIME_API_URL, TimeApiResponse.class);
            if (response != null && response.getDateTime() != null) {
                // Parse the date and time from the ISO 8601 format.
                LocalDateTime apiDateTime = LocalDateTime.parse(response.getDateTime(), DateTimeFormatter.ISO_DATE_TIME);
                logger.info("Successfully fetched date and time from TimeAPI: {}", apiDateTime);
                return apiDateTime;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch time from TimeAPI, using system time instead", e);
        }
        // Fallback: Use the local system date and time.
        LocalDateTime systemDateTime = LocalDateTime.now();
        logger.info("Using system date and time: {}", systemDateTime);
        return systemDateTime;
    }

    /**
     * Checks if the current date and time were fetched from the API or the system.
     *
     * @return true if the date was fetched from the API, false if it's the system date.
     */
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

    /**
     * Inner class to map the API response.
     */
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