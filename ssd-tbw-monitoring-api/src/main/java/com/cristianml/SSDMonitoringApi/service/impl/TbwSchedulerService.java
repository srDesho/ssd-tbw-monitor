package com.cristianml.SSDMonitoringApi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class TbwSchedulerService {

    private final TbwRecordServiceImpl tbwRecordService;
    private final TimeService timeService;
    private static final Logger logger = LoggerFactory.getLogger(TbwSchedulerService.class);

    // Start and end time for the scheduler.
    private final LocalTime startTime = LocalTime.of(17, 0); // 17:00
    private final LocalTime endTime = LocalTime.of(0, 0);    // 00:00 (midnight)

    // Control flags.
    private boolean shouldRunScheduler = false; // Initially disabled until initialization.
    private LocalDate lastRegistrationDate = null;

    public TbwSchedulerService(TbwRecordServiceImpl tbwRecordService, TimeService timeService) {
        this.tbwRecordService = tbwRecordService;
        this.timeService = timeService;
    }

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

    @Scheduled(cron = "0 * * * * ?")
    public void scheduleAutoRegisterTBW() {
        try {
            LocalDateTime currentDateTime = timeService.getCurrentDateTime();
            LocalDate currentDate = currentDateTime.toLocalDate();
            LocalTime currentTime = currentDateTime.toLocalTime();

            // Check if it's a new day.
            if (lastRegistrationDate != null && !currentDate.isEqual(lastRegistrationDate)) {
                logger.info("New day detected. Reactivating scheduler.");
                shouldRunScheduler = true;
                lastRegistrationDate = null; // Reset the last registration date.
            }

            if (!shouldRunScheduler) {
                logger.debug("Scheduler is disabled. Skipping execution.");
                return;
            }

            // Check if current time is within allowed range or if it's a new day.
            if (isWithinScheduleTime(currentTime) || (lastRegistrationDate == null)) {
                logger.info("Executing scheduler at: {}", currentTime);

                boolean tbwRegistered = tbwRecordService.autoRegisterTBW();

                if (tbwRegistered) {
                    logger.info("TBW registered successfully. Disabling scheduler until tomorrow at 17:00.");
                    shouldRunScheduler = false;
                    lastRegistrationDate = currentDate;
                }
            } else {
                logger.info("Outside allowed time range (17:00 - 00:00). Disabling scheduler.");
                shouldRunScheduler = false;
            }
        } catch (Exception e) {
            logger.error("Error during scheduler execution", e);
        }
    }

    private boolean isWithinScheduleTime(LocalTime now) {
        if (startTime.isBefore(endTime)) {
            return now.isAfter(startTime) && now.isBefore(endTime);
        } else {
            return now.isAfter(startTime) || now.isBefore(endTime);
        }
    }

    @Scheduled(cron = "0 0 17 * * ?")
    public void enableScheduler() {
        logger.info("Re-enabling scheduler for daily execution.");
        shouldRunScheduler = true;
    }
}