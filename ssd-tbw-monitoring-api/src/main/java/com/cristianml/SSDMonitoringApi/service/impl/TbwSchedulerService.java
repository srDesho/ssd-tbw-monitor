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

/**
 * This class handles the scheduling of TBW (Total Bytes Written) registration tasks.
 * It ensures that TBW records are automatically registered within a specific time range (17:00 - 00:00)
 * and handles day changes to reactivate the scheduler.
 */
@Service
public class TbwSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(TbwSchedulerService.class);

    private final TbwRecordServiceImpl tbwRecordService;
    private final TimeService timeService;
    private final TbwRecordRepository tbwRecordRepository;
    private final SSDServiceImpl ssdService; // Inject SSDServiceImpl

    // Start and end time for the scheduler.
    private static final LocalTime START_TIME = LocalTime.of(17, 0); // 17:00
    private static final LocalTime END_TIME = LocalTime.of(0, 0);    // 00:00 (midnight)

    // Control flags.
    private boolean shouldRunScheduler = false; // Initially disabled until initialization.
    private LocalDate lastRegistrationDate = null;

    public TbwSchedulerService(TbwRecordServiceImpl tbwRecordService, TimeService timeService, TbwRecordRepository tbwRecordRepository, SSDServiceImpl ssdService) {
        this.tbwRecordService = tbwRecordService;
        this.timeService = timeService;
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdService = ssdService;
    }

    /**
     * Initializes the scheduler when the application starts.
     * Checks if the server started within the allowed time range (17:00 - 00:00).
     * If so, enables the scheduler; otherwise, disables it.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Initializing TBW Scheduler Service");

        try {
            // Detect and register SSDs on startup
            ssdService.detectAndRegisterSsdOnStartup(); // Call the new method

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

    /**
     * Scheduled task that runs every minute to check if the TBW registration should be executed.
     * It ensures that the scheduler only runs within the allowed time range (17:00 - 00:00)
     * and handles day changes to reactivate the scheduler.
     */
    @Scheduled(cron = "0 */1 * * * *")     //fixedRate = 20000
    @Transactional
    public void scheduleAutoRegisterTBW() {
        logger.debug("Starting scheduled TBW registration check");

        try {
            LocalDateTime currentDateTime = timeService.getCurrentDateTime();
            LocalDate currentDate = currentDateTime.toLocalDate();
            LocalTime currentTime = currentDateTime.toLocalTime();

            // Delete future records before attempting to register TBW.
            deleteFutureRecords();

            // Verify records exist for current date
            boolean recordsExist = this.tbwRecordRepository.existsByDate(currentDate);
            if (!recordsExist) {
                logger.warn("No TBW records found for date: {}. Skipping update.", currentDate);
            } else {
                logger.debug("Checking and updating TBW records for date: {}", currentDate);
                tbwRecordService.checkAndUpdateTbwRecords(currentDate);
                return;
            }

            // Schedule time validation
            if (!isWithinScheduleTime(currentTime)) {
                logger.debug("Current time {} is outside allowed range (17:00 - 00:00)", currentTime);
                return;
            }

            // System date validation
            if (!isSystemDateValid()) {
                logger.warn("System date validation failed. Current system time: {}", LocalDateTime.now());
                return;
            }

            // TBW Registration
            boolean tbwRegistered = tbwRecordService.autoRegisterTBW();
            logger.info("TBW registration attempt completed. Success: {}", tbwRegistered);

        } catch (Exception e) {
            logger.error("Failed to execute scheduled TBW registration", e);
        }
    }

    /**
     * Checks if the current time is within the allowed schedule range (17:00 - 00:00).
     *
     * @param now the current time to check.
     * @return true if the current time is within the allowed range, false otherwise.
     */
    private boolean isWithinScheduleTime(LocalTime now) {
        boolean isWithinRange = START_TIME.isBefore(END_TIME) ?
                (now.isAfter(START_TIME) && now.isBefore(END_TIME)) :
                (now.isAfter(START_TIME) || now.isBefore(END_TIME));

        logger.debug("Time check - Current: {}, Within range: {}", now, isWithinRange);
        return isWithinRange;
    }

    /**
     * Scheduled task that runs daily at 17:00 to re-enable the scheduler.
     * This ensures that the scheduler is activated every day at the start of the allowed time range.
     */
    @Scheduled(cron = "0 0 17 * * ?")
    public void enableScheduler() {
        logger.info("Daily scheduler activation triggered at {}", LocalTime.now());
        shouldRunScheduler = true;
    }

    /**
     * Validates the system date by comparing it with the API date.
     * If the system date is manipulated (ahead or behind the API date), it returns false.
     *
     * @return true if the system date is valid, false otherwise.
     */
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

    /**
     * Deletes future records from the database.
     */
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
            throw e; // Propagar para rollback de transacción
        }
    }
}