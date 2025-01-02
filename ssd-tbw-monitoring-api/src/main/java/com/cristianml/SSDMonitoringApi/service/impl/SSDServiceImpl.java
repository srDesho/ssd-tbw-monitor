package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.SSDMapper;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.service.ISSDService;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// This class handles the business logic for managing SSDs (Solid State Drives).
// It provides methods to retrieve all SSDs and register new SSDs with validation.

@Service
public class SSDServiceImpl implements ISSDService {

    private final SSDMapper ssdMapper;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;

    // Constructor that initializes the SSDRepository dependency.
    public SSDServiceImpl(SSDMapper ssdMapper, SSDRepository ssdRepository, IHardwareService hardwareService) {
        this.ssdMapper = ssdMapper;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
    }

    // Retrieves all SSD records from the database.
    @Override
    public List<SSDResponseDTO> findAll() {
        List<SSDEntity> ssdEntityList = ssdRepository.findAll();
        return ssdMapper.toSSDResponseDTOList(ssdEntityList);
    }

    // Registers a new SSD after ensuring the model is not already registered.
    @Override
    public void detectAndRegisterSsd() {
        List<SSDResponseDTO> ssdDetectes = this.hardwareService.detectSSDs();

        for (SSDResponseDTO ssd : ssdDetectes) {
            // Validates if the SSD model already exists.
            if (this.ssdRepository.existsByModel(ssd.getModel())) {
                throw new IllegalArgumentException("SDD already registered.");
            }

            // Formats the current date and time into a readable format.
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = Utilities.formatLocalDateTime(now);

            // Creates a new SSD entity with the given model, capacity, and registration date.
            SSDEntity ssdEntity = SSDEntity.builder()
                    .model(ssd.getModel())
                    .capacityGB(ssd.getCapacityGB())
                    .registrationDate(now)
                    .isMonitored(false)
                    .build();

            // Saves the newly created SSD to the database.
            ssdMapper.toResponseDTO(this.ssdRepository.save(ssdEntity));
        }

    }

    @Override
    public void toggleMonitoring(Long id, boolean monitor) {
        SSDEntity ssd = ssdRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("SSD Not found."));
        ssd.setIsMonitored(monitor);
        this.ssdRepository.save(ssd);
    }
}