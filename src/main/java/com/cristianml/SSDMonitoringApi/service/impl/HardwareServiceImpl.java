package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Service implementation for hardware detection and SMART data retrieval
// Handles SSD discovery using smartctl command-line utility and TBW value extraction
// Provides graceful failure handling for disconnected devices with automatic monitoring disablement
@Service
public class HardwareServiceImpl implements IHardwareService {

    private static final Logger logger = LoggerFactory.getLogger(HardwareServiceImpl.class);

    private final SSDRepository ssdRepository;
    private final TbwRecordRepository tbwRecordRepository;

    public HardwareServiceImpl(SSDRepository ssdRepository, TbwRecordRepository tbwRecordRepository) {
        this.ssdRepository = ssdRepository;
        this.tbwRecordRepository = tbwRecordRepository;
    }

    // Scans system storage devices using smartctl command-line utility
    // Returns list of detected SSDs with model, serial, and capacity information
    @Override
    @Transactional(readOnly = true)
    public List<SSDResponseDTO> detectSSDsUsingSmartctl() {
        logger.debug("Starting SSD detection with smartctl");
        List<SSDResponseDTO> detectedSSDs = new ArrayList<>();

        try {
            // Execute smartctl scan command to list available storage devices
            Process process = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String device = parts[0];
                    logger.debug("Checking device: {}", device);

                    try {
                        // Get detailed information for each detected device
                        Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                        BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                        StringBuilder fullOutput = new StringBuilder();
                        String model = null;
                        String serial = null;

                        while ((line = infoReader.readLine()) != null) {
                            fullOutput.append(line).append("\n");

                            if (line.contains("Model Family:") || line.contains("Model Number:")) {
                                model = line.split(":")[1].trim();
                            }
                            if (line.contains("Serial Number:")) {
                                serial = line.split(":")[1].trim();
                            }
                        }

                        // Extract storage capacity from smartctl output
                        Long capacityGB = extractCapacityInGB(fullOutput.toString());

                        if (model != null && serial != null && capacityGB != null && capacityGB > 0) {
                            detectedSSDs.add(SSDResponseDTO.builder()
                                    .model(model)
                                    .serial(serial)
                                    .capacityGB(capacityGB)
                                    .registrationDate(LocalDateTime.now())
                                    .formattedDateTime(Utilities.formatLocalDateTime(LocalDateTime.now()))
                                    .build());
                            logger.info("Detected SSD - Model: {}, Serial: {}, Capacity: {} GB", model, serial, capacityGB);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to get info for device {}: {}", device, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while detecting SSDs", e);
            throw new RuntimeException("Error while detecting SSDs", e);
        }

        logger.info("Found {} SSDs total", detectedSSDs.size());
        return detectedSSDs;
    }

    // Parses smartctl output to extract storage capacity in gigabytes
    // Supports both SATA (User Capacity) and NVMe (Namespace Size) formats
    private Long extractCapacityInGB(String smartctlOutput) {
        try {
            // Pattern for SATA drive capacity extraction
            Pattern userPattern = Pattern.compile("User Capacity:\\s+[\\d,]+\\s+bytes\\s+\\[([\\d.]+)\\s+(TB|GB)\\]");
            Matcher userMatcher = userPattern.matcher(smartctlOutput);

            if (userMatcher.find()) {
                double size = Double.parseDouble(userMatcher.group(1));
                String unit = userMatcher.group(2);
                return Math.round("TB".equals(unit) ? size * 1024 : size);
            }

            // Pattern for NVMe drive capacity extraction
            Pattern nvmePattern = Pattern.compile("Namespace 1 Size/Capacity:\\s+[\\d,]+\\s+\\[([\\d.]+)\\s+(TB|GB)\\]");
            Matcher nvmeMatcher = nvmePattern.matcher(smartctlOutput);

            if (nvmeMatcher.find()) {
                double size = Double.parseDouble(nvmeMatcher.group(1));
                String unit = nvmeMatcher.group(2);
                return Math.round("TB".equals(unit) ? size * 1024 : size);
            }

            logger.warn("Could not extract capacity from smartctl output");
            return 0L;

        } catch (Exception e) {
            logger.error("Error extracting capacity: {}", e.getMessage());
            return 0L;
        }
    }

    // Retrieves Total Bytes Written (TBW) value for specified SSD model
    // Returns -1 if SSD is unavailable and automatically disables monitoring
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long getTBWFromSMART(String ssdModel) {
        logger.debug("Getting TBW for SSD: {}", ssdModel);

        try {
            // Scan for all available storage devices
            Process scanProcess = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader scanReader = new BufferedReader(new InputStreamReader(scanProcess.getInputStream()));

            String line;
            String matchingDevice = null;

            // Find device path that matches the requested SSD model
            while ((line = scanReader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String device = parts[0];

                    Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                    BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                    String model = null;
                    while ((line = infoReader.readLine()) != null) {
                        if (line.contains("Model Family:") || line.contains("Model Number:")) {
                            model = line.split(":")[1].trim();
                            break;
                        }
                    }

                    if (model != null && model.equalsIgnoreCase(ssdModel)) {
                        matchingDevice = device;
                        logger.debug("Found matching device: {}", device);
                        break;
                    }
                }
            }

            // If no matching device found, SSD is likely disconnected
            if (matchingDevice == null) {
                logger.warn("Device not found for model: {}. Disabling monitoring.", ssdModel);
                disableMonitoringForSsd(ssdModel);
                return -1;
            }

            // Get detailed SMART attributes including TBW data
            ProcessBuilder builder = new ProcessBuilder("smartctl", "-A", matchingDevice);
            builder.redirectErrorStream(true);
            Process tbwProcess = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(tbwProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Data Units Written")) {
                        String value = line.split(":")[1].trim().split(" ")[0].replace(",", "");
                        long dataUnitsWritten = Long.parseLong(value);

                        long totalBytesWritten = (long) (dataUnitsWritten * 512 * 931.4);
                        long tbwInGB = totalBytesWritten / (1000 * 1000 * 1000);
                        logger.info("Got TBW: {} GB for SSD: {}", tbwInGB, ssdModel);
                        return tbwInGB;
                    }
                }
            }

            int exitCode = tbwProcess.waitFor();
            if (exitCode != 0) {
                logger.warn("smartctl failed with code: {} for device: {}. Disabling monitoring.", exitCode, matchingDevice);
                disableMonitoringForSsd(ssdModel);
                return -1;
            }

        } catch (Exception e) {
            logger.warn("Failed to get TBW for SSD: {} - Device might be disconnected. Disabling monitoring.", ssdModel);
            disableMonitoringForSsd(ssdModel);
            return -1;
        }

        logger.warn("No TBW data found for SSD: {}", ssdModel);
        return -1;
    }

    // Disables monitoring for specified SSD model when device becomes unavailable
    // Runs in separate transaction to prevent affecting other operations
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void disableMonitoringForSsd(String model) {
        try {
            SSDEntity ssd = ssdRepository.findByModel(model);
            if (ssd != null && ssd.getIsMonitored()) {
                ssd.setIsMonitored(false);
                ssdRepository.save(ssd);
                logger.info("Automatically disabled monitoring for unavailable SSD: {}", model);
            }
        } catch (Exception e) {
            logger.error("Error disabling monitoring for SSD: {}", model, e);
        }
    }
}