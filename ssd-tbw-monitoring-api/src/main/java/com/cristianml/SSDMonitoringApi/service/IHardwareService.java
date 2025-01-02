package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;

import java.util.List;

public interface IHardwareService {

    long getTBW(String diskModel);
    List<String> getAvailableSSDs();
    long getDiskCapacity(String diskModel);

    List<SSDResponseDTO> detectSSDs();

}
