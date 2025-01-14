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
    public List<TbwRecordResponseDTO> findAll() {
        return this.tbwRecordMapper.toTbwRecordResponseDTOList(this.tbwRecordRepository.findAll());
    }

    /**
     * Automatically registers TBW records for all monitored SSDs if no record exists for the current date.
     * This method ensures that TBW is registered for each SSD individually, even if some SSDs already have records.
     *
     * @return true if at least one TBW record was registered, false otherwise.
     */
    @Override
    public boolean autoRegisterTBW() {
        logger.info("Executing autoRegisterTBW...");

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        // Get all monitored SSDs.
        List<SSDEntity> ssdList = ssdRepository.findByIsMonitored(true);
        boolean anyRegistered = false;

        // Register TBW for each monitored SSD if no record exists for the current date.
        for (SSDEntity ssd : ssdList) {
            Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

            if (existingRecord.isEmpty()) {
                logger.info("Registering TBW for SSD: {}", ssd.getModel());
                registerTBW(ssd, currentDate, currentTime);
                anyRegistered = true; // Indicates that at least one TBW was registered.
            } else {
                logger.info("TBW already registered today for SSD: {}", ssd.getModel());
            }
        }

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
    public TbwRecordResponseDTO manualRegisterTBW(Long ssdId, LocalDate date, LocalTime time, Long tbw) {
        // Retrieve the SSD entity by ID, or throw an exception if not found.
        SSDEntity ssd = ssdRepository.findById(ssdId)
                .orElseThrow(() -> new IllegalArgumentException("SSD with id " + ssdId + " not found"));

        // Check if a record already exists for the given SSD and date.
        Optional<TbwRecordEntity> existingRecord = this.tbwRecordRepository.findBySsdAndDate(ssd, date);
        if (existingRecord.isPresent()) {
            throw new IllegalArgumentException("A record already exists for this SSD on this date.");
        }

        // Save the TBW record and return it.
        return this.tbwRecordMapper.toResponseDTO(saveTbwRecord(ssd, date, time, tbw));
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
        // Retrieve the TBW value for the given SSD model from the hardware service.
        Long tbw = this.hardwareService.getTBWFromSMART(ssd.getModel());

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
        // Build a new TBW record entity with the provided details.
        TbwRecordEntity tbwRecord = TbwRecordEntity.builder()
                .ssd(ssd)
                .date(date)
                .time(time)
                .tbw(tbw)
                .build();

        // Persist the TBW record to the database and return it.
        return this.tbwRecordRepository.save(tbwRecord);
    }
}