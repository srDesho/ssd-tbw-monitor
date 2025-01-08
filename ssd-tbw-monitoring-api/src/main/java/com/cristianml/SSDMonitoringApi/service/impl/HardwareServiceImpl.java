package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Implementation of the hardware service, which allows detecting the connected SSD devices and obtaining the Total Bytes Written (TBW) value of a specific model.
// The Total Bytes Written (TBW) value of a specific model. This service uses the command “smartctl”
// command to interact with the disks and retrieve the necessary information. It is part of the business layer in the
// architecture.

@Service
public class HardwareServiceImpl implements IHardwareService {

    private final SSDRepository ssdRepository;
    private final TbwRecordRepository tbwRecordRepository;

    // Constructor to inject the necessary dependencies for accessing SSD and TBW data repositories
    public HardwareServiceImpl(SSDRepository ssdRepository, TbwRecordRepository tbwRecordRepository) {
        this.ssdRepository = ssdRepository;
        this.tbwRecordRepository = tbwRecordRepository;
    }

    /**
     * Detects all connected SSD devices using the `smartctl --scan` command, then retrieves
     * additional device details such as the model and capacity using the `smartctl -i` command.
     * The information is then returned as a list of SSDResponseDTO objects.
     *
     * @return a list of SSDResponseDTO containing the details of the detected SSDs
     */
    @Override
    public List<SSDResponseDTO> detectSSDsUsingSmartctl() {
        List<SSDResponseDTO> detectedSSDs = new ArrayList<>();
        try {
            // Executes the "smartctl --scan" command to get the list of connected devices
            Process process = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Each line contains information like "/dev/sdX -d <driver> ..."
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String device = parts[0]; // Example: /dev/sda
                    System.out.println("DEVICE = " + device);

                    // Executes "smartctl -i" to get information about the device
                    Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                    BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                    String model = null;
                    String serial = null;
                    String capacity = null;
                    while ((line = infoReader.readLine()) != null) {
                        if (line.contains("Model Family:") || line.contains("Model Number:")) {
                            model = line.split(":")[1].trim();
                            System.out.println("MODEL = " + model);
                        }
                        if (line.contains("Serial Number:")) {
                            serial = line.split(":")[1].trim();
                        }

                        if (line.contains("Namespace 1 Formatted LBA Size:")) {
                            capacity = line.split(":")[1].trim().split(" ")[0]; // Extracts the capacity in GB
                            System.out.println("CAPACITY = " + capacity);
                        }
                    }

                    // If model, serial and capacity are found, add them to the DTO list
                    if (model != null && serial != null && capacity != null) {
                        detectedSSDs.add(SSDResponseDTO.builder()
                                .model(model)
                                .serial(serial)
                                .capacityGB(Long.parseLong(capacity))
                                .registrationDate(LocalDateTime.now())  // Set the current date for registration
                                .formattedDateTime(Utilities.formatLocalDateTime(LocalDateTime.now())) // Format the date
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error detecting SSDs using smartctl", e);
        }

        return detectedSSDs;
    }

    /**
     * Retrieves the Total Bytes Written (TBW) value for a given SSD model.
     * It searches for the device using `smartctl --scan`, matches the device based on the provided model,
     * and then executes the "smartctl -A" command to retrieve the TBW data.
     *
     * @param ssdModel the model of the SSD for which the TBW is to be retrieved
     * @return the TBW value in GB
     */
    @Override
    public long getTBWFromSMART(String ssdModel) {
        try {
            // Step 1: Execute the "smartctl --scan" to get the list of connected devices
            Process scanProcess = Runtime.getRuntime().exec("smartctl --scan");
            BufferedReader scanReader = new BufferedReader(new InputStreamReader(scanProcess.getInputStream()));

            String line;
            String matchingDevice = null;

            // Step 2: Search for the device that matches the model
            while ((line = scanReader.readLine()) != null) {
                String[] parts = line.split(" ");
                System.out.println("SCAN LINE: " + line);
                if (parts.length > 0) {
                    String device = parts[0]; // Example: "/dev/sda"
                    System.out.println("DEVICE: " + device);

                    // Execute "smartctl -i" to retrieve device information
                    Process infoProcess = Runtime.getRuntime().exec("smartctl -i " + device);
                    BufferedReader infoReader = new BufferedReader(new InputStreamReader(infoProcess.getInputStream()));

                    String model = null;
                    while ((line = infoReader.readLine()) != null) {
                        if (line.contains("Model Family:") || line.contains("Model Number:")) {
                            model = line.split(":")[1].trim();
                            break; // No need to continue reading if we already have the model
                        }
                    }

                    // If the model matches, store the device path
                    if (model != null && model.equalsIgnoreCase(ssdModel)) {
                        matchingDevice = device;
                        System.out.println("MATCHING DEVICE: " + device);
                        break;
                    }
                }
            }

            // If no matching device is found, throw an exception
            if (matchingDevice == null) {
                throw new IllegalArgumentException("No device found with model: " + ssdModel);
            }

            // Step 3: Execute "smartctl -A" to get the TBW value for the matched device
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

                        // Convert data units to GB
                        long totalBytesWritten = (long) (dataUnitsWritten * 512 * 931.4); // 512 KB * 1024 bytes
                        return totalBytesWritten / (1000 * 1000 * 1000); // Convert to GB
                    }
                }
            }

            int exitCode = tbwProcess.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("smartctl failed with exit code: " + exitCode + " for device: " + matchingDevice);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing smartctl for model " + ssdModel, e);
        }

        return 0;
    }

}
