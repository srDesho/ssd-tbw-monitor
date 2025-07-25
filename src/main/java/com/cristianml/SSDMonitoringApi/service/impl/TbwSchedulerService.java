package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Manages scheduled TBW registration with time-based execution windows
// Ensures automatic data collection during specified daily time range (17:00 - 00:00)
@Service
public class TbwSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(TbwSchedulerService.class);

    private final TbwRecordServiceImpl tbwRecordService;
    private final TimeService timeService;
    private final TbwRecordRepository tbwRecordRepository;
    private final SSDServiceImpl ssdService;

    // Daily execution window boundaries for TBW registration
    private static final LocalTime START_TIME = LocalTime.of(17, 0); // 5:00 PM
    private static final LocalTime END_TIME = LocalTime.of(0, 0);    // 12:00 AM (midnight)

    // Control flag to enable/disable scheduler based on time validation
    private boolean shouldRunScheduler = false;

    public TbwSchedulerService(TbwRecordServiceImpl tbwRecordService, TimeService timeService, TbwRecordRepository tbwRecordRepository, SSDServiceImpl ssdService) {
        this.tbwRecordService = tbwRecordService;
        this.timeService = timeService;
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdService = ssdService;
    }

    // Initializes scheduler on application startup
    // Detects available SSDs and validates current time against execution window
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Initializing TBW Scheduler Service");

        try {
            // Register all detected SSDs on application startup
            ssdService.detectAndRegisterSsdOnStartup();

            LocalDateTime now = timeService.getCurrentDateTime();
            LocalTime currentTime = now.toLocalTime();

            shouldRunScheduler = isWithinScheduleTime(currentTime);
            logger.info("Scheduler initialization completed. Status: {}", shouldRunScheduler ? "ENABLED" : "DISABLED");

            if (shouldRunScheduler) {
                logger.info("Server started within allowed time range (17:00 - 00:00)");
            } else {
                logger.info("Server started outside allowed time range, scheduler will activate at next window");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize scheduler", e);
            shouldRunScheduler = false;
        }
    }

    // Main scheduled task executed every minute
    // Manages TBW registration workflow with validation checks
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void scheduleAutoRegisterTBW() {
        logger.debug("Starting scheduled TBW registration check");

        try {
            LocalDateTime currentDateTime = timeService.getCurrentDateTime();
            LocalDate currentDate = currentDateTime.toLocalDate();
            LocalTime currentTime = currentDateTime.toLocalTime();

            // Clean up any future-dated records before processing
            deleteFutureRecords();

            // Check if records already exist for current date
            boolean recordsExist = this.tbwRecordRepository.existsByDate(currentDate);
            if (!recordsExist) {
                logger.warn("No TBW records found for date: {}. Skipping update.", currentDate);
            } else {
                logger.debug("Checking and updating TBW records for date: {}", currentDate);
                tbwRecordService.checkAndUpdateTbwRecords(currentDate);
                return;
            }

            // Validate current time is within allowed execution window
            if (!isWithinScheduleTime(currentTime)) {
                logger.debug("Current time {} is outside allowed range (17:00 - 00:00)", currentTime);
                return;
            }

            // Validate system date hasn't been manipulated
            if (!isSystemDateValid()) {
                logger.warn("System date validation failed. Current system time: {}", LocalDateTime.now());
                return;
            }

            // Execute automatic TBW registration for all monitored SSDs
            boolean tbwRegistered = tbwRecordService.autoRegisterTBW();
            logger.info("TBW registration attempt completed. Success: {}", tbwRegistered);

        } catch (Exception e) {
            logger.error("Failed to execute scheduled TBW registration", e);
        }
    }

    // Determines if current time falls within daily execution window
    // Handles midnight crossing (17:00 to 00:00 next day)
    private boolean isWithinScheduleTime(LocalTime now) {
        boolean isWithinRange = START_TIME.isBefore(END_TIME) ?
                (now.isAfter(START_TIME) && now.isBefore(END_TIME)) :
                (now.isAfter(START_TIME) || now.isBefore(END_TIME));

        logger.debug("Time check - Current: {}, Within range: {}", now, isWithinRange);
        return isWithinRange;
    }

    // Daily reactivation task executed at 17:00
    // Enables scheduler for next execution window
    @Scheduled(cron = "0 0 17 * * ?")
    public void enableScheduler() {
        logger.info("Daily scheduler activation triggered at {}", LocalTime.now());
        shouldRunScheduler = true;
    }

    // Validates system date integrity by comparing with external time API
    // Prevents TBW registration if system clock appears manipulated
    private boolean isSystemDateValid() {
        try {
            LocalDateTime systemDateTime = LocalDateTime.now();
            LocalDateTime apiDateTime = timeService.getCurrentDateTime();

            boolean isValid = systemDateTime.toLocalDate().isEqual(apiDateTime.toLocalDate());
            logger.debug("Date validation - System: {}, API: {}, Valid: {}",
                    systemDateTime.toLocalDate(), apiDateTime.toLocalDate(), isValid);
            return isValid;
        } catch (Exception e) {
            logger.error("System date validation failed", e);
            return false;
        }
    }

    // Removes any future-dated TBW records from database
    // Maintains data integrity by preventing records with future timestamps
    @Transactional
    public void deleteFutureRecords() {
        logger.debug("Starting future records cleanup");

        try {
            if (!timeService.isApiDateAvailable()) {
                logger.warn("API date unavailable, skipping future records cleanup");
                return;
            }

            LocalDate apiDate = timeService.getCurrentDateTime().toLocalDate();
            List<TbwRecordEntity> futureRecords = tbwRecordRepository.findByDateAfter(apiDate);

            if (!futureRecords.isEmpty()) {
                logger.info("Found {} future records to delete", futureRecords.size());
                tbwRecordRepository.deleteAll(futureRecords);
                logger.info("Successfully deleted {} future records", futureRecords.size());
            } else {
                logger.debug("No future records found to delete");
            }
        } catch (Exception e) {
            logger.error("Failed to delete future records", e);
            throw e;
        }
    }
}