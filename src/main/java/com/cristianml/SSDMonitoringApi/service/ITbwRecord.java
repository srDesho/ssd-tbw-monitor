package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ITbwRecord {

    List<TbwRecordResponseDTO> findAll();
    boolean autoRegisterTBW();

    long getCurrentTbwForSSD(Long ssdId);
}
