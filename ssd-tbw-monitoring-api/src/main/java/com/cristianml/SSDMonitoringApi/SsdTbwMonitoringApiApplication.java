package com.cristianml.SSDMonitoringApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
public class SsdTbwMonitoringApiApplication {

	public static void main(String[] args) {

		// Create data folder if not exists.
		File dataDir = new File("./data");
		if (!dataDir.exists()) {
			dataDir.mkdirs();
			System.out.println("Database directory created: " + dataDir.getAbsolutePath());
		}

		SpringApplication.run(SsdTbwMonitoringApiApplication.class, args);
	}

}
