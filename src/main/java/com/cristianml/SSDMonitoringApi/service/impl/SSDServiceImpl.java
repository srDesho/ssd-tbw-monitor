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

// Service implementation for SSD device management and registration
// Handles SSD detection, database registration, and monitoring status control
// Provides startup initialization and manual monitoring toggle functionality
@Service
public class SSDServiceImpl implements ISSDService {

    private static final Logger logger = LoggerFactory.getLogger(SSDServiceImpl.class);

    // Dependencies for data mapping, database operations, and hardware communication
    private final SSDMapper ssdMapper;
    private final SSDRepository ssdRepository;
    private final IHardwareService hardwareService;

    // Constructor for dependency injection of required components
    public SSDServiceImpl(SSDMapper ssdMapper, SSDRepository ssdRepository, IHardwareService hardwareService) {
        this.ssdMapper = ssdMapper;
        this.ssdRepository = ssdRepository;
        this.hardwareService = hardwareService;
    }

    // Retrieves all SSD entities from database and converts to response DTOs
    // Used for displaying SSD inventory in user interface
    @Override
    @Transactional(readOnly = true)
    public List<SSDResponseDTO> findAll() {
        logger.debug("Retrieving all SSD entities");
        List<SSDEntity> ssdEntityList = ssdRepository.findAll();
        logger.info("Found {} SSD entities", ssdEntityList.size());
        return ssdMapper.toSSDResponseDTOList(ssdEntityList);
    }

    // Detects available SSDs using hardware service and registers new devices
    // Updates monitoring status for existing SSDs and creates records for new ones
    @Override
    @Transactional
    public void detectAndRegisterSsd() {
        logger.debug("Starting SSD detection and registration process");

        // Use hardware service to scan for connected storage devices
        List<SSDResponseDTO> detectedSSDs = hardwareService.detectSSDsUsingSmartctl();
        logger.info("Detected {} SSDs", detectedSSDs.size());

        for (SSDResponseDTO ssd : detectedSSDs) {
            try {
                // Check if SSD already exists in database using unique model-serial combination
                SSDEntity existingSsd = this.ssdRepository.findByModelAndSerial(ssd.getModel(), ssd.getSerial());

                if (existingSsd != null) {
                    logger.warn("SSD with model {} and serial {} is already registered", ssd.getModel(), ssd.getSerial());
                    // Enable monitoring if previously disabled
                    if (!existingSsd.getIsMonitored()) {
                        existingSsd.setIsMonitored(true);
                        this.ssdRepository.save(existingSsd);
                        logger.info("Updated monitoring status to true for SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial());
                    }
                } else {
                    // Create new SSD entity for previously unregistered device
                    LocalDateTime now = LocalDateTime.now();

                    SSDEntity ssdEntity = SSDEntity.builder()
                            .model(ssd.getModel())
                            .serial(ssd.getSerial())
                            .capacityGB(ssd.getCapacityGB())
                            .registrationDate(now)
                            .isMonitored(false) // Initially false until explicitly enabled
                            .build();

                    // Persist new entity and convert to response format
                    SSDEntity savedEntity = this.ssdRepository.save(ssdEntity);
                    ssdMapper.toResponseDTO(savedEntity);
                    logger.info("Successfully registered new SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial());
                }
            } catch (Exception e) {
                logger.error("Error registering SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial(), e);
                throw e;
            }
        }
    }

    // Detects and registers SSDs during application startup
    // Sets initial monitoring status to true for new devices detected at startup
    @Transactional
    public void detectAndRegisterSsdOnStartup() {
        logger.debug("Starting SSD detection and registration process on startup");

        List<SSDResponseDTO> detectedSSDs = hardwareService.detectSSDsUsingSmartctl();
        logger.info("Detected {} SSDs", detectedSSDs.size());

        for (SSDResponseDTO ssd : detectedSSDs) {
            try {
                SSDEntity existingSsd = this.ssdRepository.findByModelAndSerial(ssd.getModel(), ssd.getSerial());

                if (existingSsd != null) {
                    logger.warn("SSD with model {} and serial {} is already registered", ssd.getModel(), ssd.getSerial());
                    // Enable monitoring for existing SSDs found during startup
                    if (!existingSsd.getIsMonitored()) {
                        existingSsd.setIsMonitored(true);
                        this.ssdRepository.save(existingSsd);
                        logger.info("Updated monitoring status to true for SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial());
                    }
                } else {
                    // Create new SSD entity with monitoring enabled by default for startup detection
                    LocalDateTime now = LocalDateTime.now();

                    SSDEntity ssdEntity = SSDEntity.builder()
                            .model(ssd.getModel())
                            .serial(ssd.getSerial())
                            .capacityGB(ssd.getCapacityGB())
                            .registrationDate(now)
                            .isMonitored(true) // Enabled by default for startup detection
                            .build();

                    SSDEntity savedEntity = this.ssdRepository.save(ssdEntity);
                    ssdMapper.toResponseDTO(savedEntity);
                    logger.info("Successfully registered new SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial());
                }
            } catch (Exception e) {
                logger.error("Error registering SSD: model={}, serial={}", ssd.getModel(), ssd.getSerial(), e);
                throw e;
            }
        }
    }

    // Enables or disables monitoring for specific SSD identified by ID
    // Used for manual control of TBW data collection for individual drives
    @Override
    @Transactional
    public void toggleMonitoring(Long id, boolean monitor) {
        logger.debug("Attempting to toggle monitoring status to {} for SSD with ID: {}", monitor, id);

        try {
            // Retrieve SSD entity or throw exception if not found
            SSDEntity ssd = ssdRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("SSD Not found."));

            // Update monitoring status and persist changes
            ssd.setIsMonitored(monitor);
            this.ssdRepository.save(ssd);
            logger.info("Successfully updated monitoring status to {} for SSD: {}", monitor, ssd.getModel());
        } catch (Exception e) {
            logger.error("Error toggling monitoring status for SSD with ID: {}", id, e);
            throw e;
        }
    }
}