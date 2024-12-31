package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ITbwRecord {

    List<TbwRecordResponseDTO> findAll();
    void autoRegisterTBW();
    TbwRecordResponseDTO manualRegisterTBW(Long ssdId, LocalDate date, LocalTime time, Long tbw);

}
