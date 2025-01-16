package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.SSDMapper;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.service.ISSDService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * This class implements the business logic for managing SSDs.
 * It provides methods to detect, register, retrieve, and manage monitoring status for SSD entities.
 */
@Service
public class SSDServiceImpl implements ISSDService {

    private static final Logger logger = LoggerFactory.getLogger(SSDServiceImpl.class);

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

    /**
     * Retrieves all SSD entities from the database and maps them to response DTOs.
     *
     * @return a list of SSDResponseDTO containing the details of the SSDs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SSDResponseDTO> findAll() {
        logger.debug("Retrieving all SSD entities");
        List<SSDEntity> ssdEntityList = ssdRepository.findAll();
        logger.info("Found {} SSD entities", ssdEntityList.size());
        return ssdMapper.toSSDResponseDTOList(ssdEntityList);
    }

    /**
     * Detects SSDs using hardware services and registers them if they are not already in the database.
     */
    @Override
    @Transactional
    public void detectAndRegisterSsd() {
        logger.debug("Starting SSD detection and registration process");

        // Detect SSDs using the hardware service.
        List<SSDResponseDTO> detectedSSDs = hardwareService.detectSSDsUsingSmartctl();
        logger.info("Detected {} SSDs", detectedSSDs.size());

        for (SSDResponseDTO ssd : detectedSSDs) {
            try {
                // Check if the SSD is already registered.
                if (this.ssdRepository.existsByModelAndSerial(ssd.getModel(), ssd.getSerial())) {
                    logger.warn("SSD with model {} and serial {} is already registered", ssd.getModel(), ssd.getSerial());
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
                SSDEntity savedEntity = this.ssdRepository.save(ssdEntity);
                ssdMapper.toResponseDTO(savedEntity);
                logger.info("Successfully registered new SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial());
            } catch (Exception e) {
                logger.error("Error registering SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial(), e);
                throw e;
            }
        }
    }

    /**
     * Toggles the monitoring status of a specific SSD by its ID.
     *
     * @param id      the ID of the SSD to toggle monitoring.
     * @param monitor the new monitoring status (true to enable, false to disable).
     */
    @Override
    @Transactional
    public void toggleMonitoring(Long id, boolean monitor) {
        logger.debug("Attempting to toggle monitoring status to {} for SSD with ID: {}", monitor, id);

        try {
            // Retrieve the SSD entity by its ID or throw an exception if not found.
            SSDEntity ssd = ssdRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("SSD Not found."));

            // Update the monitoring status and save the entity.
            ssd.setIsMonitored(monitor);
            this.ssdRepository.save(ssd);
            logger.info("Successfully updated monitoring status to {} for SSD: {}", monitor, ssd.getModel());
        } catch (Exception e) {
            logger.error("Error toggling monitoring status for SSD with ID: {}", id, e);
            throw e;
        }
    }
}