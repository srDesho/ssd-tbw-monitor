package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;

import java.util.List;

public interface ISSDService {

    List<SSDResponseDTO> findAll();
    SSDResponseDTO registerSsd(String model, Long capacity);
}
