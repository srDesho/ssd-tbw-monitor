package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class HardwareServiceImpl implements IHardwareService {

    private static final Logger logger = LoggerFactory.getLogger(HardwareServiceImpl.class);

    private final SSDRepository ssdRepository;
    private final TbwRecordRepository tbwRecordRepository;

    public HardwareServiceImpl(SSDRepository ssdRepository, TbwRecordRepository tbwRecordRepository) {
        this.ssdRepository = ssdRepository;
        this.tbwRecordRepository = tbwRecordRepository;
    }

    /**
     * Finds all connected SSD drives using smartctl command and gets their details like model, serial, and capacity.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SSDResponseDTO> detectSSDsUsingSmartctl() {
        logger.debug("Starting to look for SSDs with smartctl");
        List<SSDResponseDTO> detectedSSDs = new ArrayList<>();

        try {
            // Run smartctl scan to find all storage devices
            Process process = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Each line has device info like "/dev/sda -d sat"
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String device = parts[0]; // Gets the device path like /dev/sda
                    logger.debug("Checking device: {}", device);

                    try {
                        // Get detailed info for this device
                        Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                        BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                        String model = null;
                        String serial = null;
                        String capacity = null;
                        while ((line = infoReader.readLine()) != null) {
                            if (line.contains("Model Family:") || line.contains("Model Number:")) {
                                model = line.split(":")[1].trim();
                                logger.debug("Found model: {}", model);
                            }
                            if (line.contains("Serial Number:")) {
                                serial = line.split(":")[1].trim();
                            }
                            if (line.contains("Namespace 1 Formatted LBA Size:")) {
                                capacity = line.split(":")[1].trim().split(" ")[0];
                                logger.debug("Found capacity: {}", capacity);
                            }
                        }

                        // If we found all the basic info, create the SSD object
                        if (model != null && serial != null && capacity != null) {
                            detectedSSDs.add(SSDResponseDTO.builder()
                                    .model(model)
                                    .serial(serial)
                                    .capacityGB(Long.parseLong(capacity))
                                    .registrationDate(LocalDateTime.now())
                                    .formattedDateTime(Utilities.formatLocalDateTime(LocalDateTime.now()))
                                    .build());
                            logger.info("Detected SSD - Model: {}, Serial: {}", model, serial);
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

    /**
     * Gets the TBW (Total Bytes Written) value for a specific SSD model by checking SMART data.
     */
    @Override
    @Transactional(readOnly = true)
    public long getTBWFromSMART(String ssdModel) {
        logger.debug("Getting TBW for SSD: {}", ssdModel);

        try {
            // First, scan for all devices
            Process scanProcess = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader scanReader = new BufferedReader(new InputStreamReader(scanProcess.getInputStream()));

            String line;
            String matchingDevice = null;

            // Look through all devices to find the one that matches our model
            while ((line = scanReader.readLine()) != null) {
                String[] parts = line.split(" ");
                logger.debug("Checking device line: {}", line);
                if (parts.length > 0) {
                    String device = parts[0]; // Device path like /dev/sda
                    logger.debug("Testing device: {}", device);

                    // Check what model this device is
                    Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                    BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                    String model = null;
                    while ((line = infoReader.readLine()) != null) {
                        if (line.contains("Model Family:") || line.contains("Model Number:")) {
                            model = line.split(":")[1].trim();
                            break;
                        }
                    }

                    // If this is the SSD we're looking for, remember the device path
                    if (model != null && model.equalsIgnoreCase(ssdModel)) {
                        matchingDevice = device;
                        logger.debug("Found matching device: {}", device);
                        break;
                    }
                }
            }

            // If we didn't find the SSD, throw error
            if (matchingDevice == null) {
                logger.warn("Couldn't find device with model: {}", ssdModel);
                throw new IllegalArgumentException("Couldn't find device with model: " + ssdModel);
            }

            // Now get the TBW data from the specific device
            ProcessBuilder builder = new ProcessBuilder(
                    "smartctl",
                    "-A",
                    matchingDevice
            );

            builder.redirectErrorStream(true);
            Process tbwProcess = builder.start();

            // Read the output to find the TBW value
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(tbwProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Data Units Written")) {
                        String value = line.split(":")[1].trim().split(" ")[0].replace(",", "");
                        long dataUnitsWritten = Long.parseLong(value);

                        // Convert the smartctl units to GB
                        long totalBytesWritten = (long) (dataUnitsWritten * 512 * 931.4);
                        long tbwInGB = totalBytesWritten / (1000 * 1000 * 1000);
                        logger.info("Got TBW: {}GB for SSD: {}", tbwInGB, ssdModel);
                        return tbwInGB;
                    }
                }
            }

            int exitCode = tbwProcess.waitFor();
            if (exitCode != 0) {
                logger.error("smartctl command failed with code: {} for device: {}", exitCode, matchingDevice);
                throw new RuntimeException("smartctl command failed with code: " + exitCode + " for device: " + matchingDevice);
            }
        } catch (Exception e) {
            logger.error("Error running smartctl for model {}", ssdModel, e);
            throw new RuntimeException("Error running smartctl for model " + ssdModel, e);
        }

        logger.warn("No TBW data found for SSD: {}", ssdModel);
        return 0;
    }
}