package com.cristianml.SSDMonitoringApi.service;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;

import java.util.List;

public interface ISSDService {

    List<SSDEntity> findAll();
    SSDEntity registerSsd(String model, Long capacity);
}
