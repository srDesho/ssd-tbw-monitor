package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.service.ISSDService;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class SSDServiceImpl implements ISSDService {

    private final SSDRepository ssdRepository;

    public SSDServiceImpl(SSDRepository ssdRepository) {
        this.ssdRepository = ssdRepository;
    }


    @Override
    public List<SSDEntity> findAll() {
        return ssdRepository.findAll();
    }

    @Override
    public SSDEntity registerSsd(String model, Long capacity) {
        if (this.ssdRepository.existsByModel(model)) {
            throw new IllegalArgumentException("SDD already registered.");
        }

        SSDEntity ssd = SSDEntity.builder()
                .model(model)
                .capacityGB(capacity)
                .registrationDate(LocalDateTime.now())
                .build();
        return this.ssdRepository.save(ssd);
    }
}
