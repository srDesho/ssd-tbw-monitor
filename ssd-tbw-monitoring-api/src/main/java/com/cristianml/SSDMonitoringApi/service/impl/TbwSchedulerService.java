package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    private final TbwRecordServiceImpl tbwRecordService;
    private final TimeService timeService;
    private static final Logger logger = LoggerFactory.getLogger(TbwSchedulerService.class);
    private final TbwRecordRepository tbwRecordRepository;

    // Start and end time for the scheduler.
    private final LocalTime startTime = LocalTime.of(17, 0); // 17:00
    private final LocalTime endTime = LocalTime.of(0, 0);    // 00:00 (midnight)

    // Control flags.
    private boolean shouldRunScheduler = false; // Initially disabled until initialization.
    private LocalDate lastRegistrationDate = null;

    public TbwSchedulerService(TbwRecordServiceImpl tbwRecordService, TimeService timeService, TbwRecordRepository tbwRecordRepository) {
        this.tbwRecordService = tbwRecordService;
        this.timeService = timeService;
        this.tbwRecordRepository = tbwRecordRepository;
    }

    /**
     * Initializes the scheduler when the application starts.
     * Checks if the server started within the allowed time range (17:00 - 00:00).
     * If so, enables the scheduler; otherwise, disables it.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            LocalDateTime now = timeService.getCurrentDateTime();
            LocalTime currentTime = now.toLocalTime();

            if (isWithinScheduleTime(currentTime)) {
                logger.info("Server started within the allowed time range (17:00 - 00:00). Enabling scheduler.");
                shouldRunScheduler = true;
            } else {
                logger.info("Server started outside the allowed time range. Disabling scheduler.");
                shouldRunScheduler = false;
            }
        } catch (Exception e) {
            logger.error("Error during scheduler initialization", e);
            shouldRunScheduler = false;
        }
    }

    /**
     * Scheduled task that runs every minute to check if the TBW registration should be executed.
     * It ensures that the scheduler only runs within the allowed time range (17:00 - 00:00)
     * and handles day changes to reactivate the scheduler.
     */
    @Scheduled(fixedRate = 2000) // cron = "0 * * * * ?" Runs every minute
    public void scheduleAutoRegisterTBW() {
        try {
            LocalDateTime currentDateTime = timeService.getCurrentDateTime();
            LocalDate currentDate = currentDateTime.toLocalDate();
            LocalTime currentTime = currentDateTime.toLocalTime();

            // Check if the current time is within the allowed range (17:00 - 00:00).
            if (!isWithinScheduleTime(currentTime)) {
                logger.info("Outside allowed time range (17:00 - 00:00). Skipping execution.");
                return;
            }

            // Delete future records before attempting to register TBW.
            deleteFutureRecords();

            // Validate system date before proceeding with TBW registration.
            if (!isSystemDateValid()) {
                logger.warn("System date is manipulated. Skipping TBW registration.");
                return;
            }

            // Attempt to register TBW.
            boolean tbwRegistered = tbwRecordService.autoRegisterTBW();

            if (tbwRegistered) {
                logger.info("TBW registered for at least one SSD.");
            } else {
                logger.info("No TBW registration needed at this moment.");
            }

        } catch (Exception e) {
            logger.error("Error during scheduler execution", e);
        }
    }

    /**
     * Checks if the current time is within the allowed schedule range (17:00 - 00:00).
     *
     * @param now the current time to check.
     * @return true if the current time is within the allowed range, false otherwise.
     */
    private boolean isWithinScheduleTime(LocalTime now) {
        if (startTime.isBefore(endTime)) {
            return now.isAfter(startTime) && now.isBefore(endTime);
        } else {
            return now.isAfter(startTime) || now.isBefore(endTime);
        }
    }

    /**
     * Scheduled task that runs daily at 17:00 to re-enable the scheduler.
     * This ensures that the scheduler is activated every day at the start of the allowed time range.
     */
    @Scheduled(cron = "0 0 17 * * ?")
    public void enableScheduler() {
        logger.info("Re-enabling scheduler for daily execution.");
        shouldRunScheduler = true; // Re-enable the scheduler.
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

            // If the system date is ahead or behind the API date, return false.
            return systemDateTime.toLocalDate().isEqual(apiDateTime.toLocalDate());
        } catch (Exception e) {
            logger.error("Error while validating system date", e);
            return false; // Assume the date is invalid if there's an error.
        }
    }

    /**
     * Deletes TBW records with dates that are ahead of the current API date.
     * This ensures that no future records are kept in the database.
     * Only deletes future records if the API date is available and the system date is valid.
     */
    public void deleteFutureRecords() {
        try {
            // Check if the API date is available.
            if (!timeService.isApiDateAvailable()) {
                logger.warn("API date is not available. Skipping deletion of future records.");
                return;
            }

            // Get the current date from the API.
            LocalDateTime apiDateTime = timeService.getCurrentDateTime();
            LocalDate apiDate = apiDateTime.toLocalDate();

            // Get the current system date.
            LocalDate systemDate = LocalDate.now();

            // Only delete future records if the system date is valid (matches the API date).
            if (systemDate.isEqual(apiDate)) {
                // Find and delete records with dates after the current API date.
                List<TbwRecordEntity> futureRecords = tbwRecordRepository.findByDateAfter(apiDate);
                if (!futureRecords.isEmpty()) {
                    logger.info("Deleting {} future records", futureRecords.size());
                    tbwRecordRepository.deleteAll(futureRecords);
                }
            } else {
                logger.warn("System date is manipulated. Skipping deletion of future records.");
            }
        } catch (Exception e) {
            logger.error("Error while deleting future records", e);
        }
    }
}