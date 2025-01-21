package com.cristianml.SSDMonitoringApi;

import com.cristianml.SSDMonitoringApi.config.DatabaseInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SsdTbwMonitoringApiApplication {

	public static void main(String[] args) {

		// Check and create DB if it doesn't exist.
		DatabaseInitializer.initializeDatabase();

		SpringApplication.run(SsdTbwMonitoringApiApplication.class, args);
	}

}
