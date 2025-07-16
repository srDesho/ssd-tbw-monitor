package com.cristianml.SSDMonitoringApi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Monitors system clock for manipulation and triggers application restart if detected.
 * Prevents inconsistent state when system time is changed backward or forward unexpectedly.
 */
@Service
public class ClockMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(ClockMonitorService.class);
    private final ApplicationContext applicationContext;

    // Tolerance: 2 minutes difference is considered clock manipulation
    private static final long MAX_TIME_DRIFT_SECONDS = 120;

    private Instant lastCheckTime;

    public ClockMonitorService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.lastCheckTime = Instant.now();
        logger.info("Clock monitor initialized at {}", lastCheckTime);
    }

    /**
     * Checks every minute if system clock has been manipulated.
     * If time jump exceeds threshold, triggers application restart.
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void checkClockIntegrity() {
        Instant currentTime = Instant.now();
        Duration elapsed = Duration.between(lastCheckTime, currentTime);
        long elapsedSeconds = elapsed.getSeconds();

        // Expected: ~60 seconds between checks
        // If difference is > 120 seconds, clock was manipulated
        if (Math.abs(elapsedSeconds - 60) > MAX_TIME_DRIFT_SECONDS) {
            logger.error("CLOCK MANIPULATION DETECTED! Expected ~60s, got {}s. Restarting application...",
                    elapsedSeconds);
            restartApplication();
            return;
        }

        logger.debug("Clock integrity check passed. Elapsed: {}s", elapsedSeconds);
        lastCheckTime = currentTime;
    }

    /**
     * Triggers graceful application restart.
     */
    private void restartApplication() {
        logger.warn("Initiating application restart due to clock manipulation");

        new Thread(() -> {
            try {
                Thread.sleep(1000); // Brief delay for log flush
                System.exit(10); // Exit code 10 indicates restart required
            } catch (InterruptedException e) {
                logger.error("Restart interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}