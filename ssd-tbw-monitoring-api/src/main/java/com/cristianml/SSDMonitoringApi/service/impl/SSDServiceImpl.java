package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.service.ISSDService;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// This class handles the business logic for managing SSDs (Solid State Drives).
// It provides methods to retrieve all SSDs and register new SSDs with validation.

@Service
public class SSDServiceImpl implements ISSDService {

    private final SSDRepository ssdRepository;

    // Constructor that initializes the SSDRepository dependency.
    public SSDServiceImpl(SSDRepository ssdRepository) {
        this.ssdRepository = ssdRepository;
    }

    // Retrieves all SSD records from the database.
    @Override
    public List<SSDEntity> findAll() {
        return ssdRepository.findAll();
    }

    // Registers a new SSD after ensuring the model is not already registered.
    @Override
    public SSDEntity registerSsd(String model, Long capacity) {
        // Validates if the SSD model already exists.
        if (this.ssdRepository.existsByModel(model)) {
            throw new IllegalArgumentException("SDD already registered.");
        }

        // Creates a new SSD entity with the given model, capacity, and registration date.
        SSDEntity ssd = SSDEntity.builder()
                .model(model)
                .capacityGB(capacity)
                .registrationDate(LocalDateTime.now())
                .build();

        // Saves the newly created SSD to the database.
        return this.ssdRepository.save(ssd);
    }
}