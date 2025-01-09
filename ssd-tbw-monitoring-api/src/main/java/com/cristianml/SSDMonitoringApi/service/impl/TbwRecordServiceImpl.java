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

// This class implements the business logic for managing TBW (Total Bytes Written) records.
// It includes methods to retrieve, automatically register, and manually register TBW entries for SSDs.
// Additionally, it handles time-based logic for auto-registering TBW records.
@Service
public class TbwRecordServiceImpl implements ITbwRecord {

    // Repositories and mappers used for database access and data transformation.
    private final TbwRecordRepository tbwRecordRepository;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;
    private final TbwRecordMapper tbwRecordMapper;

    // Logger for logging important information.
    private static final Logger logger = LoggerFactory.getLogger(TbwRecordServiceImpl.class);

    // Predefined time for automatic TBW registration.
    private final LocalTime autoRegisterTime = LocalTime.of(16, 0);

    // Constructor for dependency injection.
    public TbwRecordServiceImpl(TbwRecordRepository tbwRecordRepository, SSDRepository ssdRepository, IHardwareService hardwareService, TbwRecordMapper tbwRecordMapper) {
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
        this.tbwRecordMapper = tbwRecordMapper;
    }

    // Retrieves all TBW records from the database and maps them to response DTOs.
    @Override
    public List<TbwRecordResponseDTO> findAll() {
        return this.tbwRecordMapper.toTbwRecordResponseDTOList(this.tbwRecordRepository.findAll());
    }

    // Automatically registers TBW records for all SSDs if certain time conditions are met.
    @Override
    public boolean autoRegisterTBW() {
        logger.info("Executing autoRegisterTBW...");

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        // Checks if the TBW has already been registered for the current day.
        List<SSDEntity> ssdList = ssdRepository.findByIsMonitored(true);
        boolean alreadyRegistered = ssdList.stream()
                .anyMatch(ssd -> tbwRecordRepository.findBySsdAndDate(ssd, currentDate).isPresent());

        if (alreadyRegistered) {
            logger.info("TBW already registered for today.");
            return true; // Indicates that TBW is already registered.
        }

        // Registers TBW for monitored SSDs.
        logger.info("Registering TBW for monitored SSDs...");
        for (SSDEntity ssd : ssdList) {
            Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

            if (existingRecord.isEmpty()) {
                logger.info("Registering TBW for SSD: {}", ssd.getModel());
                registerTBW(ssd, currentDate, currentTime);
                return true; // Indicates that TBW was successfully registered.
            }
        }

        return false; // Indicates that TBW was not registered.
    }

    // Manually registers a TBW record for a specific SSD and date.
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

    // Registers a TBW record for a given SSD, retrieving the current TBW value from the hardware service.
    private TbwRecordEntity registerTBW(SSDEntity ssd, LocalDate date, LocalTime time) {
        // Retrieve the TBW value for the given SSD model from the hardware service.
        Long tbw = this.hardwareService.getTBWFromSMART(ssd.getModel());

        // Save the TBW record.
        return saveTbwRecord(ssd, date, time, tbw);
    }

    // Saves a TBW record to the repository with the provided SSD, date, time, and TBW value.
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