package com.cristianml.SSDMonitoringApi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TimeService {

    private static final String TIME_API_URL = "https://timeapi.io/api/Time/current/zone?timeZone=America/La_Paz";
    private static final Logger logger = LoggerFactory.getLogger(TimeService.class);

    // Obtiene la fecha y hora actuales desde la API.
    public LocalDateTime getCurrentDateTime() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            TimeApiResponse response = restTemplate.getForObject(TIME_API_URL, TimeApiResponse.class);
            if (response != null && response.getDateTime() != null) {
                // Parsear la fecha y hora desde el formato ISO 8601.
                return LocalDateTime.parse(response.getDateTime(), DateTimeFormatter.ISO_DATE_TIME);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch time from TimeAPI, using system time instead", e);
        }
        // Fallback: Usar la fecha y hora del sistema local.
        return LocalDateTime.now();
    }

    // Clase interna para mapear la respuesta de la API.
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