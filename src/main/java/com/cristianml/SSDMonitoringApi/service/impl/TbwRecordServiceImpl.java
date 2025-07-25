package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.TbwRecordMapper;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.service.ITbwRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

// Service implementation for TBW (Total Bytes Written) record management
// Handles automatic registration, updating, and retrieval of SSD write endurance data
// Processes each SSD independently to prevent single device failures from affecting others
@Service
public class TbwRecordServiceImpl implements ITbwRecord {

    private final TbwRecordRepository tbwRecordRepository;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;
    private final TbwRecordMapper tbwRecordMapper;

    private static final Logger logger = LoggerFactory.getLogger(TbwRecordServiceImpl.class);

    // Threshold for TBW update detection (3 GB in bytes)
    private static final long TBW_UPDATE_THRESHOLD = 3L * 1024 * 1024 * 1024;

    public TbwRecordServiceImpl(TbwRecordRepository tbwRecordRepository, SSDRepository ssdRepository, IHardwareService hardwareService, TbwRecordMapper tbwRecordMapper) {
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
        this.tbwRecordMapper = tbwRecordMapper;
    }

    // Retrieves all TBW records from database for reporting and display
    @Override
    @Transactional(readOnly = true)
    public List<TbwRecordResponseDTO> findAll() {
        logger.debug("Retrieving all TBW records");
        List<TbwRecordEntity> tbwRecords = this.tbwRecordRepository.findAll();
        logger.info("Found {} TBW records", tbwRecords.size());
        return this.tbwRecordMapper.toTbwRecordResponseDTOList(tbwRecords);
    }

    // Automatically registers TBW for all monitored SSDs that don't have today's record
    // Prevents registration if system date appears manipulated (delayed)
    @Override
    public boolean autoRegisterTBW() {
        logger.info("Executing autoRegisterTBW...");

        // Check for potential date manipulation by comparing with latest record
        Optional<TbwRecordEntity> higherDateRecordOpt = this.tbwRecordRepository.findTopByOrderByDateDesc();

        if (higherDateRecordOpt.isPresent()) {
            TbwRecordEntity higherDateRecord = higherDateRecordOpt.get();
            if (LocalDate.now().isBefore(higherDateRecord.getDate())) {
                logger.warn("System date manipulated, date delayed. Skipping TBW registration.");
                return false;
            }
        } else {
            logger.info("No previous TBW records found. Proceeding with first registration.");
        }

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        logger.debug("Current date: {}, Current time: {}", currentDate, currentTime);

        // Get all SSDs currently marked for monitoring
        List<SSDEntity> ssdList = ssdRepository.findByIsMonitored(true);
        logger.info("Found {} monitored SSDs", ssdList.size());

        boolean anyRegistered = false;

        // Process each SSD independently to prevent single failure from blocking others
        for (SSDEntity ssd : ssdList) {
            try {
                boolean registered = processSsdRegistration(ssd, currentDate, currentTime);
                if (registered) {
                    anyRegistered = true;
                }
            } catch (Exception e) {
                logger.error("Error processing SSD: {}. Continuing with others.", ssd.getModel(), e);
            }
        }

        logger.info("autoRegisterTBW completed. Registered new TBW records: {}", anyRegistered);
        return anyRegistered;
    }

    // Registers TBW record for individual SSD in separate transaction
    // Returns false if SSD is unavailable (-1) or already registered today
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processSsdRegistration(SSDEntity ssd, LocalDate currentDate, LocalTime currentTime) {
        try {
            Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

            if (existingRecord.isEmpty()) {
                logger.info("Registering TBW for SSD: {}", ssd.getModel());

                // Get current TBW value from hardware service
                long tbw = hardwareService.getTBWFromSMART(ssd.getModel());

                // Check for hardware failure indication (-1 means SSD unavailable)
                if (tbw == -1) {
                    logger.warn("Skipped TBW registration for unavailable SSD: {}", ssd.getModel());
                    return false;
                }

                logger.info("Retrieved TBW value: {} for SSD: {}", tbw, ssd.getModel());

                // Create and persist new TBW record
                TbwRecordEntity tbwRecord = TbwRecordEntity.builder()
                        .ssd(ssd)
                        .date(currentDate)
                        .time(currentTime)
                        .tbw(tbw)
                        .build();

                tbwRecordRepository.save(tbwRecord);
                logger.info("Successfully saved TBW record for SSD: {}", ssd.getModel());
                return true;
            } else {
                logger.info("TBW already registered today for SSD: {}", ssd.getModel());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error registering TBW for SSD: {}", ssd.getModel(), e);
            return false;
        }
    }

    // Retrieves current TBW value for specific SSD by ID
    // Used for manual queries and real-time status checks
    @Override
    @Transactional(readOnly = true)
    public long getCurrentTbwForSSD(Long ssdId) {
        logger.debug("Retrieving current TBW for SSD ID: {}", ssdId);

        try {
            SSDEntity ssd = ssdRepository.findById(ssdId)
                    .orElseThrow(() -> {
                        logger.error("SSD with ID {} not found", ssdId);
                        return new IllegalArgumentException("SSD with id " + ssdId + " not found");
                    });

            long tbw = this.hardwareService.getTBWFromSMART(ssd.getModel());
            logger.info("Retrieved current TBW value: {} for SSD ID: {}", tbw, ssdId);
            return tbw;
        } catch (Exception e) {
            logger.error("Error retrieving TBW for SSD ID: {}", ssdId, e);
            throw e;
        }
    }

    // Checks and updates existing TBW records if significant increase detected
    // Compares current hardware reading with stored value for each SSD
    public void checkAndUpdateTbwRecords(LocalDate currentDate) {
        logger.info("Running checkAndUpdateTbwRecords method for date: {}", currentDate);

        try {
            List<SSDEntity> ssdList = ssdRepository.findByIsMonitored(true);
            logger.info("Found {} monitored SSDs", ssdList.size());

            // Process each SSD update independently
            for (SSDEntity ssd : ssdList) {
                try {
                    processSsdUpdate(ssd, currentDate);
                } catch (Exception e) {
                    logger.error("Error processing update for SSD: {}. Continuing with others.", ssd.getModel(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error while checking and updating TBW records", e);
        }
    }

    // Updates existing TBW record if current value exceeds stored value by threshold
    // Runs in separate transaction to maintain data consistency
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSsdUpdate(SSDEntity ssd, LocalDate currentDate) {
        try {
            Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

            if (existingRecord.isPresent()) {
                TbwRecordEntity record = existingRecord.get();
                long recordedTbw = record.getTbw();
                logger.debug("Recorded TBW for SSD {}: {}", ssd.getModel(), recordedTbw);

                long currentTbw = hardwareService.getTBWFromSMART(ssd.getModel());

                // Skip update if SSD is currently unavailable
                if (currentTbw == -1) {
                    logger.warn("Skipped update for unavailable SSD: {}", ssd.getModel());
                    return;
                }

                logger.debug("Current TBW for SSD {}: {}", ssd.getModel(), currentTbw);

                // Update record only if significant increase detected (3+ GB)
                if (currentTbw - recordedTbw >= TBW_UPDATE_THRESHOLD) {
                    logger.info("Updating TBW record for SSD: {} on date: {}", ssd.getModel(), currentDate);
                    record.setTbw(currentTbw);
                    tbwRecordRepository.save(record);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating TBW for SSD: {}", ssd.getModel(), e);
        }
    }
}