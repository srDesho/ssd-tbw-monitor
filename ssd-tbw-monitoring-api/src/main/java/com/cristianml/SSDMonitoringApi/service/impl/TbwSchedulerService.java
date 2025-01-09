package com.cristianml.SSDMonitoringApi.service.impl;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class TbwSchedulerService {

    private final TbwRecordServiceImpl tbwRecordService;
    private static final Logger logger = LoggerFactory.getLogger(TbwSchedulerService.class);

    // Scheduler start and end times.
    private final LocalTime startTime = LocalTime.of(17, 0); // 17:00
    private final LocalTime endTime = LocalTime.of(0, 0);    // 00:00 (midnight)

    public TbwSchedulerService(TbwRecordServiceImpl tbwRecordService) {
        this.tbwRecordService = tbwRecordService;
    }

    // Executes the scheduler every minute.
    @Scheduled(cron = "0 * * * * ?")
    public void scheduleAutoRegisterTBW() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        // Checks if the current time is within the allowed range (17:00 - 00:00).
        if (isWithinScheduleTime(now)) {
            logger.info("Executing scheduler at: {}", now);

            // Attempts to register TBW.
            boolean tbwRegistered = tbwRecordService.autoRegisterTBW();

            // If TBW is registered, stop the scheduler.
            if (tbwRegistered) {
                logger.info("TBW registered. Scheduler stopped.");
                // Additional logic to stop the scheduler could be added here if necessary.
            }
        } else {
            logger.info("Outside execution time range (17:00 - 00:00).");
        }
    }

    // Checks if the current time is within the allowed range.
    private boolean isWithinScheduleTime(LocalTime now) {
        if (startTime.isBefore(endTime)) {
            // Normal case: 17:00 - 00:00
            return now.isAfter(startTime) && now.isBefore(endTime);
        } else {
            // Special case: If endTime is 00:00, it is considered the start of the next day.
            return now.isAfter(startTime) || now.isBefore(endTime);
        }
    }

    @PostConstruct
    public void init() {
        LocalTime now = LocalTime.now();

        // If the server starts after 17:00, execute the scheduler immediately.
        if (isWithinScheduleTime(now)) {
            logger.info("Server started within execution time. Running scheduler...");
            scheduleAutoRegisterTBW();
        }
    }
}