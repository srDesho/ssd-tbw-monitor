package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.SSDMapper;
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

    private final SSDMapper ssdMapper;
    private final SSDRepository ssdRepository;

    // Constructor that initializes the SSDRepository dependency.
    public SSDServiceImpl(SSDMapper ssdMapper, SSDRepository ssdRepository) {
        this.ssdMapper = ssdMapper;
        this.ssdRepository = ssdRepository;
    }

    // Retrieves all SSD records from the database.
    @Override
    public List<SSDResponseDTO> findAll() {
        List<SSDEntity> ssdEntityList = ssdRepository.findAll();
        return ssdMapper.toSSDResponseDTOList(ssdEntityList);
    }

    // Registers a new SSD after ensuring the model is not already registered.
    @Override
    public SSDResponseDTO registerSsd(String model, Long capacity) {
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
        return ssdMapper.toResponseDTO(this.ssdRepository.save(ssd));
    }
}