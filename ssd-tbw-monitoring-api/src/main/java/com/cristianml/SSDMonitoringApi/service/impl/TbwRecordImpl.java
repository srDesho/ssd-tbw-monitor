package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
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

@Service
public class TbwRecordImpl implements ITbwRecord {

    private final TbwRecordRepository tbwRecordRepository;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;

    // Hour defined to automatic register
    private final LocalTime autoRegisterTime = LocalTime.of(17, 0);

    public TbwRecordImpl(TbwRecordRepository tbwRecordRepository, SSDRepository ssdRepository, IHardwareService hardwareService) {
        this.tbwRecordRepository = tbwRecordRepository;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
    }

    @Override
    public List<TbwRecordEntity> findAll() {
        return this.tbwRecordRepository.findAll();
    }

    @Override
    public void autoRegisterTBW() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime= LocalTime.now();
        LocalDateTime configuredDateTime = LocalDateTime.of(currentDate, this.autoRegisterTime);

        // If is before than configured time
        if (now.isBefore(configuredDateTime)) {
            return;
        }

        // If midnight of the next day
        LocalDate nextDay = currentDate.plusDays(1);
        LocalDateTime starOfNextDay = LocalDateTime.of(nextDay, LocalTime.MIDNIGHT);
        if (now.isAfter(starOfNextDay)) {
            return;
        }

        // findAll ssd registers
        List<SSDEntity> ssdList = ssdRepository.findAll();

        for (SSDEntity ssd : ssdList) {
            // Verify if exists a register with current date
            Optional<TbwRecordEntity> existingRecord = tbwRecordRepository.findBySsdAndDate(ssd, currentDate);

            if (existingRecord.isEmpty()) {
                // Register TBW with current time
                registerTBW(ssd, currentDate, currentTime);
            }
        }
    }

    @Override
    public TbwRecordEntity manualRegisterTBW(Long ssdId, LocalDate date, LocalTime time, Long tbw) {
        // Get SSD existing
        SSDEntity ssd = ssdRepository.findById(ssdId)
                .orElseThrow(() -> new IllegalArgumentException("SSD with id " + ssdId + " not found"));

        // Verify if already exists a register with date and ssd.
        Optional<TbwRecordEntity> existingRecord = this.tbwRecordRepository.findBySsdAndDate(ssd, date);
        if (existingRecord.isPresent()) {
            throw new IllegalArgumentException("A record already exists for this SSD on this date.");
        }

        return saveTbwRecord(ssd, date, time, tbw);
    }

    private TbwRecordEntity registerTBW(SSDEntity ssd, LocalDate date, LocalTime time) {
        // Get current TBW
        Long tbw = this.hardwareService.getTBW(ssd.getModel());

        return saveTbwRecord(ssd, date, time, tbw);
    }

    private TbwRecordEntity saveTbwRecord(SSDEntity ssd, LocalDate date, LocalTime time, Long tbw) {
        TbwRecordEntity tbwRecord = TbwRecordEntity.builder()
                .ssd(ssd)
                .date(date)
                .time(time)
                .tbw(tbw)
                .build();

        return this.tbwRecordRepository.save(tbwRecord);
    }

}
