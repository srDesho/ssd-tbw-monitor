package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.SSDMapper;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.service.ISSDService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// This class implements the business logic for managing SSDs.
// It provides methods to detect, register, retrieve, and manage monitoring status for SSD entities.
@Service
public class SSDServiceImpl implements ISSDService {

    // Dependencies used for mapping, database access, and hardware operations.
    private final SSDMapper ssdMapper;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;

    // Constructor for dependency injection.
    public SSDServiceImpl(SSDMapper ssdMapper, SSDRepository ssdRepository, IHardwareService hardwareService) {
        this.ssdMapper = ssdMapper;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
    }

    // Retrieves all SSD entities from the database and maps them to response DTOs.
    @Override
    public List<SSDResponseDTO> findAll() {
        List<SSDEntity> ssdEntityList = ssdRepository.findAll();
        return ssdMapper.toSSDResponseDTOList(ssdEntityList);
    }

    // Detects SSDs using hardware services and registers them if they are not already in the database.
    @Override
    public void detectAndRegisterSsd() {
        // Detect SSDs using the hardware service.
        List<SSDResponseDTO> detectedSSDs = hardwareService.detectSSDsUsingSmartctl();

        for (SSDResponseDTO ssd : detectedSSDs) {
            // Check if the SSD is already registered.
            if (this.ssdRepository.existsByModelAndSerial(ssd.getModel(), ssd.getSerial())) {
                throw new IllegalArgumentException("SSD already registered.");
            }

            // Get the current date and time.
            LocalDateTime now = LocalDateTime.now();

            // Build a new SSD entity and save it to the database.
            SSDEntity ssdEntity = SSDEntity.builder()
                    .model(ssd.getModel())
                    .serial(ssd.getSerial())
                    .capacityGB(ssd.getCapacityGB())
                    .registrationDate(now)
                    .isMonitored(false)
                    .build();

            // Save the entity and map it to a response DTO.
            ssdMapper.toResponseDTO(this.ssdRepository.save(ssdEntity));
        }
    }

    // Toggles the monitoring status of a specific SSD by its ID.
    @Override
    public void toggleMonitoring(Long id, boolean monitor) {
        // Retrieve the SSD entity by its ID or throw an exception if not found.
        SSDEntity ssd = ssdRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("SSD Not found."));

        // Update the monitoring status and save the entity.
        ssd.setIsMonitored(monitor);
        this.ssdRepository.save(ssd);
    }
}
