package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.TbwRecordMapper;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.service.ITbwRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

// This class manages the business logic for TBW (Total Bytes Written) records.
// It provides methods to retrieve, auto-register, and manually register TBW entries for SSDs.

@Service
public class TbwRecordServiceImpl implements ITbwRecord {

    private final TbwRecordRepository tbwRecordRepository;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;
    private final TbwRecordMapper tbwRecordMapper;

    // Predefined time for automatic TBW registration.
    private final LocalTime autoRegisterTime = LocalTime.of(16, 0);

    // Constructor that initializes the dependencies.
    public TbwRecordServiceImpl(TbwRecordRepository tbwRecordRepository, SSDRepository ssdRepository, IHardwareService hardwareService, TbwRecordMapper tbwRecordMapper) {
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
        this.tbwRecordMapper = tbwRecordMapper;
    }

    // Retrieves all TBW records from the database.
    @Override
    public List<TbwRecordResponseDTO> findAll() {
        return this.tbwRecordMapper.toTbwRecordResponseDTOList((this.tbwRecordRepository.findAll()));
    }

    // Automatically registers TBW records for all SSDs if certain time conditions are met.
    @Override
    public void autoRegisterTBW() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        LocalDateTime configuredDateTime = LocalDateTime.of(currentDate, this.autoRegisterTime);

        // Skip registration if the current time is before the configured auto-register time.
        if (now.isBefore(configuredDateTime)) {
            return;
        }

        // Skip registration if the current time is after midnight of the next day.
        LocalDate nextDay = currentDate.plusDays(1);
        LocalDateTime startOfNextDay = LocalDateTime.of(nextDay, LocalTime.MIDNIGHT);
        if (now.isAfter(startOfNextDay)) {
            return;
        }

        // Retrieves all SSDs from the repository.
        List<SSDEntity> ssdList = ssdRepository.findAll();

        // For each SSD, checks if a record exists for the current date. If not, creates a new record.
        for (SSDEntity ssd : ssdList) {
            Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

            if (existingRecord.isEmpty()) {
                registerTBW(ssd, currentDate, currentTime);
            }
        }
    }

    // Manually registers a TBW record for a specific SSD and date.
    @Override
    public TbwRecordResponseDTO manualRegisterTBW(Long ssdId, LocalDate date, LocalTime time, Long tbw) {
        // Retrieves the SSD entity by ID, or throws an exception if not found.
        SSDEntity ssd = ssdRepository.findById(ssdId)
                .orElseThrow(() -> new IllegalArgumentException("SSD with id " + ssdId + " not found"));

        // Checks if a record already exists for the given SSD and date.
        Optional<TbwRecordEntity> existingRecord = this.tbwRecordRepository.findBySsdAndDate(ssd, date);
        if (existingRecord.isPresent()) {
            throw new IllegalArgumentException("A record already exists for this SSD on this date.");
        }

        // Saves the TBW record and returns it.
        return this.tbwRecordMapper.toResponseDTO(saveTbwRecord(ssd, date, time, tbw));
    }

    // Registers a TBW record for a given SSD, retrieving the current TBW value from the hardware service.
    private TbwRecordEntity registerTBW(SSDEntity ssd, LocalDate date, LocalTime time) {
        // Retrieves the TBW value for the given SSD model from the hardware service.
        Long tbw = this.hardwareService.getTBW(ssd.getModel());

        // Saves the TBW record.
        return saveTbwRecord(ssd, date, time, tbw);
    }

    // Saves a TBW record to the repository with the provided SSD, date, time, and TBW value.
    private TbwRecordEntity saveTbwRecord(SSDEntity ssd, LocalDate date, LocalTime time, Long tbw) {
        // Builds a new TBW record entity with the provided details.
        TbwRecordEntity tbwRecord = TbwRecordEntity.builder()
                .ssd(ssd)
                .date(date)
                .time(time)
                .tbw(tbw)
                .build();

        // Persists the TBW record to the database and returns it.
        return this.tbwRecordRepository.save(tbwRecord);
    }
}