package com.cristianml.SSDMonitoringApi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

// Service implementation for system clock integrity monitoring
// Detects unexpected time changes and triggers application restart when manipulation is detected
// Prevents data corruption from system clock adjustments during TBW recording
@Service
public class ClockMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(ClockMonitorService.class);
    private final ApplicationContext applicationContext;

    // Maximum allowed time drift between scheduled checks (2 minutes)
    // Prevents false positives while detecting significant time manipulation
    private static final long MAX_TIME_DRIFT_SECONDS = 120;

    private Instant lastCheckTime;

    public ClockMonitorService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.lastCheckTime = Instant.now();
        logger.info("Clock monitor initialized at {}", lastCheckTime);
    }

    // Scheduled task that monitors system clock integrity every minute
    // Compares elapsed time between checks to detect unexpected time jumps
    @Scheduled(fixedRate = 60000) // Executes every 60 seconds
    public void checkClockIntegrity() {
        Instant currentTime = Instant.now();
        Duration elapsed = Duration.between(lastCheckTime, currentTime);
        long elapsedSeconds = elapsed.getSeconds();

        // Expected elapsed time is approximately 60 seconds between checks
        // Significant deviation indicates potential clock manipulation
        if (Math.abs(elapsedSeconds - 60) > MAX_TIME_DRIFT_SECONDS) {
            logger.error("CLOCK MANIPULATION DETECTED! Expected ~60s, got {}s. Restarting application...",
                    elapsedSeconds);
            restartApplication();
            return;
        }

        logger.debug("Clock integrity check passed. Elapsed: {}s", elapsedSeconds);
        lastCheckTime = currentTime;
    }

    // Triggers graceful application restart when clock manipulation detected
    // Uses exit code 10 to indicate restart requirement to process manager
    private void restartApplication() {
        logger.warn("Initiating application restart due to clock manipulation");

        new Thread(() -> {
            try {
                Thread.sleep(1000); // Brief delay to allow log flushing
                System.exit(10); // Special exit code for restart indication
            } catch (InterruptedException e) {
                logger.error("Restart interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}