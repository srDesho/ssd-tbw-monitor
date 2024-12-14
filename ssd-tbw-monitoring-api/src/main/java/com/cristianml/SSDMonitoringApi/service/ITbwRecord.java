package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ITbwRecord {

    List<TbwRecordEntity> findAll();
    void autoRegisterTBW();
    TbwRecordEntity manualRegisterTBW(Long ssdId, LocalDate date, LocalTime time, Long tbw);

}
