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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * This class implements the business logic for managing TBW (Total Bytes Written) records.
 * It includes methods to retrieve, automatically register, and manually register TBW entries for SSDs.
 * Additionally, it handles time-based logic for auto-registering TBW records.
 */
@Service
public class TbwRecordServiceImpl implements ITbwRecord {

    // Repositories and mappers used for database access and data transformation.
    private final TbwRecordRepository tbwRecordRepository;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;
    private final TbwRecordMapper tbwRecordMapper;

    // Logger for logging important information.
    private static final Logger logger = LoggerFactory.getLogger(TbwRecordServiceImpl.class);

    // Threshold for TBW update in bytes (3 GB).
    private static final long TBW_UPDATE_THRESHOLD = 3L * 1024 * 1024 * 1024; // 3 GB in bytes.

    // Constructor for dependency injection.
    public TbwRecordServiceImpl(TbwRecordRepository tbwRecordRepository, SSDRepository ssdRepository, IHardwareService hardwareService, TbwRecordMapper tbwRecordMapper) {
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
        this.tbwRecordMapper = tbwRecordMapper;
    }

    /**
     * Retrieves all TBW records from the database and maps them to response DTOs.
     *
     * @return a list of TBW records mapped to DTOs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<TbwRecordResponseDTO> findAll() {
        logger.debug("Retrieving all TBW records");
        List<TbwRecordEntity> tbwRecords = this.tbwRecordRepository.findAll();
        logger.info("Found {} TBW records", tbwRecords.size());
        return this.tbwRecordMapper.toTbwRecordResponseDTOList(tbwRecords);
    }

    /**
     * Automatically registers TBW records for all monitored SSDs if no record exists for the current date.
     * This method ensures that TBW is registered for each SSD individually, even if some SSDs already have records.
     *
     * @return true if at least one TBW record was registered, false otherwise.
     */
    @Override
    @Transactional
    public boolean autoRegisterTBW() {
        logger.info("Executing autoRegisterTBW...");

        // Get record with the highest date.
        TbwRecordEntity higherDateRecord = this.tbwRecordRepository.findTopByOrderByDateDesc()
                .orElseThrow(() -> {
                    logger.error("No TBW records found in the database.");
                    return new IllegalArgumentException("No records found");
                });

        // Validate if the current date is before the highest recorded date.
        if (LocalDate.now().isBefore(higherDateRecord.getDate())) {
            logger.warn("System date manipulated, date delayed. Skipping TBW registration.");
            return false;
        }

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        logger.debug("Current date: {}, Current time: {}", currentDate, currentTime);

        // Get all monitored SSDs.
        List<SSDEntity> ssdList = ssdRepository.findByIsMonitored(true);
        logger.info("Found {} monitored SSDs", ssdList.size());

        boolean anyRegistered = false;

        // Register TBW for each monitored SSD if no record exists for the current date.
        for (SSDEntity ssd : ssdList) {
            try {
                Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

                if (existingRecord.isEmpty()) {
                    logger.info("Registering TBW for SSD: {}", ssd.getModel());
                    registerTBW(ssd, currentDate, currentTime);
                    anyRegistered = true; // Indicates that at least one TBW was registered.
                } else {
                    logger.info("TBW already registered today for SSD: {}", ssd.getModel());
                }
            } catch (Exception e) {
                logger.error("Error registering TBW for SSD: {}", ssd.getModel(), e);
                throw e;
            }
        }

        logger.info("autoRegisterTBW completed. Registered new TBW records: {}", anyRegistered);
        return anyRegistered; // Return true if at least one TBW was registered, otherwise false.
    }

    /**
     * Manually registers a TBW record for a specific SSD and date.
     *
     * @param ssdId the ID of the SSD for which the TBW record is being registered.
     * @param date  the date of the TBW record.
     * @param time  the time of the TBW record.
     * @param tbw   the TBW value to be registered.
     * @return the registered TBW record mapped to a DTO.
     * @throws IllegalArgumentException if a record already exists for the given SSD and date.
     */
    @Override
    @Transactional
    public TbwRecordResponseDTO manualRegisterTBW(Long ssdId, LocalDate date, LocalTime time, Long tbw) {
        logger.debug("Attempting to manually register TBW for SSD ID: {}", ssdId);

        try {
            // Retrieve the SSD entity by ID, or throw an exception if not found.
            SSDEntity ssd = ssdRepository.findById(ssdId)
                    .orElseThrow(() -> {
                        logger.error("SSD with ID {} not found", ssdId);
                        return new IllegalArgumentException("SSD with id " + ssdId + " not found");
                    });

            // Check if a record already exists for the given SSD and date.
            Optional<TbwRecordEntity> existingRecord = this.tbwRecordRepository.findBySsdAndDate(ssd, date);
            if (existingRecord.isPresent()) {
                logger.warn("A record already exists for SSD ID: {} on date: {}", ssdId, date);
                throw new IllegalArgumentException("A record already exists for this SSD on this date.");
            }

            // Save the TBW record and return it.
            TbwRecordEntity savedRecord = saveTbwRecord(ssd, date, time, tbw);
            logger.info("Successfully registered TBW for SSD ID: {}", ssdId);
            return this.tbwRecordMapper.toResponseDTO(savedRecord);
        } catch (Exception e) {
            logger.error("Error manually registering TBW for SSD ID: {}", ssdId, e);
            throw e;
        }
    }

    /**
     * Registers a TBW record for a given SSD, retrieving the current TBW value from the hardware service.
     *
     * @param ssd  the SSD for which the TBW record is being registered.
     * @param date the date of the TBW record.
     * @param time the time of the TBW record.
     * @return the registered TBW record.
     */
    private TbwRecordEntity registerTBW(SSDEntity ssd, LocalDate date, LocalTime time) {
        logger.debug("Registering TBW for SSD: {}", ssd.getModel());

        // Retrieve the TBW value for the given SSD model from the hardware service.
        Long tbw = this.hardwareService.getTBWFromSMART(ssd.getModel());
        logger.info("Retrieved TBW value: {} for SSD: {}", tbw, ssd.getModel());

        // Save the TBW record.
        return saveTbwRecord(ssd, date, time, tbw);
    }

    /**
     * Saves a TBW record to the repository with the provided SSD, date, time, and TBW value.
     *
     * @param ssd  the SSD associated with the TBW record.
     * @param date the date of the TBW record.
     * @param time the time of the TBW record.
     * @param tbw  the TBW value to be saved.
     * @return the saved TBW record.
     */
    private TbwRecordEntity saveTbwRecord(SSDEntity ssd, LocalDate date, LocalTime time, Long tbw) {
        logger.debug("Saving TBW record for SSD: {}", ssd.getModel());

        // Build a new TBW record entity with the provided details.
        TbwRecordEntity tbwRecord = TbwRecordEntity.builder()
                .ssd(ssd)
                .date(date)
                .time(time)
                .tbw(tbw)
                .build();

        // Persist the TBW record to the database and return it.
        TbwRecordEntity savedRecord = this.tbwRecordRepository.save(tbwRecord);
        logger.info("Successfully saved TBW record for SSD: {}", ssd.getModel());
        return savedRecord;
    }

    /**
     * Retrieves the current TBW value for a specific SSD from the hardware service.
     *
     * @param ssdId the ID of the SSD for which the current TBW value is being retrieved.
     * @return the current TBW value for the specified SSD.
     */
    @Override
    @Transactional(readOnly = true)
    public long getCurrentTbwForSSD(Long ssdId) {
        logger.debug("Retrieving current TBW for SSD ID: {}", ssdId);

        try {
            // Retrieve the SSD entity by ID, or throw an exception if not found.
            SSDEntity ssd = ssdRepository.findById(ssdId)
                    .orElseThrow(() -> {
                        logger.error("SSD with ID {} not found", ssdId);
                        return new IllegalArgumentException("SSD with id " + ssdId + " not found");
                    });

            // Retrieve the TBW value for the given SSD model from the hardware service.
            long tbw = this.hardwareService.getTBWFromSMART(ssd.getModel());
            logger.info("Retrieved current TBW value: {} for SSD ID: {}", tbw, ssdId);
            return tbw;
        } catch (Exception e) {
            logger.error("Error retrieving TBW for SSD ID: {}", ssdId, e);
            throw e;
        }
    }

    /**
     * Retrieves the current TBW value for a specific SSD from the hardware service.
     *
     * @param ssd the SSD entity for which the current TBW value is being retrieved.
     * @return the current TBW value for the specified SSD.
     */
    public long getCurrentTbwForSSD(SSDEntity ssd) {
        logger.debug("Retrieving current TBW for SSD: {}", ssd.getModel());

        try {
            // Retrieve the TBW value for the given SSD model from the hardware service.
            long tbw = this.hardwareService.getTBWFromSMART(ssd.getModel());
            logger.info("Retrieved current TBW value: {} for SSD: {}", tbw, ssd.getModel());
            return tbw;
        } catch (Exception e) {
            logger.error("Error retrieving TBW for SSD: {}", ssd.getModel(), e);
            throw e;
        }
    }

    /**
     * Compares the recorded TBW with the current TBW from the hardware service and updates the record if necessary.
     * This method ensures that the TBW records are up-to-date with the actual values from the SSDs.
     *
     * @param currentDate the current date to check records for.
     */
    public void checkAndUpdateTbwRecords(LocalDate currentDate) {
        logger.info("Running checkAndUpdateTbwRecords method for date: {}", currentDate);

        try {
            // Get all monitored SSDs.
            List<SSDEntity> ssdList = ssdRepository.findByIsMonitored(true);
            logger.info("Found {} monitored SSDs", ssdList.size());

            // Iterate through each SSD and check/update its TBW record.
            for (SSDEntity ssd : ssdList) {
                try {
                    // Find the existing TBW record for the current date.
                    Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

                    if (existingRecord.isPresent()) {
                        TbwRecordEntity record = existingRecord.get();
                        long recordedTbw = record.getTbw();
                        logger.debug("Recorded TBW for SSD {}: {}", ssd.getModel(), recordedTbw);

                        // Retrieve the current TBW value from the hardware service.
                        long currentTbw = this.hardwareService.getTBWFromSMART(ssd.getModel());
                        logger.debug("Current TBW for SSD {}: {}", ssd.getModel(), currentTbw);

                        // If the current TBW is greater than or equal to the recorded TBW by 3 GB, update the record.
                        if (currentTbw - recordedTbw >= TBW_UPDATE_THRESHOLD) {
                            logger.info("Updating TBW record for SSD: {} on date: {}", ssd.getModel(), currentDate);
                            record.setTbw(currentTbw);
                            tbwRecordRepository.save(record);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error updating TBW record for SSD: {}", ssd.getModel(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error while checking and updating TBW records", e);
        }
    }
}