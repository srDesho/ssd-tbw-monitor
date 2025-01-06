package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;

import java.util.List;

public interface IHardwareService {

    List<SSDResponseDTO> detectSSDsUsingSmartctl();
    long getTBWFromSMART(String deviceName);


}
