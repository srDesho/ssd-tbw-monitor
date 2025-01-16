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

/**
 * This class implements the hardware service, which allows detecting connected SSD devices
 * and retrieving the Total Bytes Written (TBW) value for a specific model using the `smartctl` command.
 * It interacts with the disks to retrieve necessary information and is part of the business layer.
 */
@Service
public class HardwareServiceImpl implements IHardwareService {

    private static final Logger logger = LoggerFactory.getLogger(HardwareServiceImpl.class);

    private final SSDRepository ssdRepository;
    private final TbwRecordRepository tbwRecordRepository;

    // Constructor to inject the necessary dependencies for accessing SSD and TBW data repositories.
    public HardwareServiceImpl(SSDRepository ssdRepository, TbwRecordRepository tbwRecordRepository) {
        this.ssdRepository = ssdRepository;
        this.tbwRecordRepository = tbwRecordRepository;
    }

    /**
     * Detects all connected SSD devices using the `smartctl --scan` command, then retrieves
     * additional device details such as the model and capacity using the `smartctl -i` command.
     * The information is then returned as a list of SSDResponseDTO objects.
     *
     * @return a list of SSDResponseDTO containing the details of the detected SSDs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SSDResponseDTO> detectSSDsUsingSmartctl() {
        logger.debug("Starting SSD detection using smartctl");
        List<SSDResponseDTO> detectedSSDs = new ArrayList<>();

        try {
            // Executes the "smartctl --scan" command to get the list of connected devices.
            Process process = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Each line contains information like "/dev/sdX -d <driver> ..."
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String device = parts[0]; // Example: /dev/sda
                    logger.debug("Processing device: {}", device);

                    try {
                        // Executes "smartctl -i" to get information about the device.
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

                        // If model, serial, and capacity are found, add them to the DTO list.
                        if (model != null && serial != null && capacity != null) {
                            detectedSSDs.add(SSDResponseDTO.builder()
                                    .model(model)
                                    .serial(serial)
                                    .capacityGB(Long.parseLong(capacity))
                                    .registrationDate(LocalDateTime.now())
                                    .formattedDateTime(Utilities.formatLocalDateTime(LocalDateTime.now()))
                                    .build());
                            logger.info("Successfully detected SSD: model={}, serial={}", model, serial);
                        }
                    } catch (Exception e) {
                        logger.error("Error retrieving information for device {}: {}", device, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error detecting SSDs using smartctl", e);
            throw new RuntimeException("Error detecting SSDs using smartctl", e);
        }

        logger.info("Detected {} SSDs in total", detectedSSDs.size());
        return detectedSSDs;
    }

    /**
     * Retrieves the Total Bytes Written (TBW) value for a given SSD model.
     * It searches for the device using `smartctl --scan`, matches the device based on the provided model,
     * and then executes the "smartctl -A" command to retrieve the TBW data.
     *
     * @param ssdModel the model of the SSD for which the TBW is to be retrieved.
     * @return the TBW value in GB.
     */
    @Override
    @Transactional(readOnly = true)
    public long getTBWFromSMART(String ssdModel) {
        logger.debug("Retrieving TBW for SSD model: {}", ssdModel);

        try {
            // Step 1: Execute the "smartctl --scan" to get the list of connected devices.
            Process scanProcess = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader scanReader = new BufferedReader(new InputStreamReader(scanProcess.getInputStream()));

            String line;
            String matchingDevice = null;

            // Step 2: Search for the device that matches the model.
            while ((line = scanReader.readLine()) != null) {
                String[] parts = line.split(" ");
                logger.debug("Scanning line: {}", line);
                if (parts.length > 0) {
                    String device = parts[0]; // Example: "/dev/sda"
                    logger.debug("Checking device: {}", device);

                    // Execute "smartctl -i" to retrieve device information.
                    Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                    BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                    String model = null;
                    while ((line = infoReader.readLine()) != null) {
                        if (line.contains("Model Family:") || line.contains("Model Number:")) {
                            model = line.split(":")[1].trim();
                            break;
                        }
                    }

                    // If the model matches, store the device path.
                    if (model != null && model.equalsIgnoreCase(ssdModel)) {
                        matchingDevice = device;
                        logger.debug("Found matching device: {}", device);
                        break;
                    }
                }
            }

            // If no matching device is found, throw an exception.
            if (matchingDevice == null) {
                logger.warn("No device found with model: {}", ssdModel);
                throw new IllegalArgumentException("No device found with model: " + ssdModel);
            }

            // Step 3: Execute "smartctl -A" to get the TBW value for the matched device.
            ProcessBuilder builder = new ProcessBuilder(
                    "smartctl",
                    "-A",
                    matchingDevice
            );

            builder.redirectErrorStream(true);
            Process tbwProcess = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(tbwProcess.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Data Units Written")) {
                        String value = line.split(":")[1].trim().split(" ")[0].replace(",", "");
                        long dataUnitsWritten = Long.parseLong(value);

                        // Convert data units to GB.
                        long totalBytesWritten = (long) (dataUnitsWritten * 512 * 931.4);
                        long tbwInGB = totalBytesWritten / (1000 * 1000 * 1000);
                        logger.info("Retrieved TBW value of {}GB for SSD model: {}", tbwInGB, ssdModel);
                        return tbwInGB;
                    }
                }
            }

            int exitCode = tbwProcess.waitFor();
            if (exitCode != 0) {
                logger.error("smartctl failed with exit code: {} for device: {}", exitCode, matchingDevice);
                throw new RuntimeException("smartctl failed with exit code: " + exitCode + " for device: " + matchingDevice);
            }
        } catch (Exception e) {
            logger.error("Error executing smartctl for model {}", ssdModel, e);
            throw new RuntimeException("Error executing smartctl for model " + ssdModel, e);
        }

        logger.warn("No TBW value found for SSD model: {}", ssdModel);
        return 0;
    }
}