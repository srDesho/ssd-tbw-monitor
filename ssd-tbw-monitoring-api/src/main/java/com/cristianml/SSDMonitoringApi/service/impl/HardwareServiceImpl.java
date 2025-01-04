package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the Hardware Service interface that provides methods to interact with storage devices.
 * Uses OSHI library for hardware abstraction and SMART commands for detailed SSD metrics.
 */
@Service
public class HardwareServiceImpl implements IHardwareService {

    /**
     * Retrieves the Total Bytes Written (TBW) for a specified disk model.
     * Converts raw bytes to gigabytes for standardized reporting.
     *
     * @param diskModel The model name of the disk to query
     * @return Total bytes written in GB
     * @throws IllegalArgumentException if no disk matches the specified model
     */
    @Override
    public long getTBW(String diskModel) {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            if ((disk.getModel().split("\\(")[0].trim()).equalsIgnoreCase(diskModel)) {

                System.out.println("Serial Number ::::::::::::: " + disk.getSerial());
                System.out.println("Disk Name ::::::::::::: " + disk.getName());
                System.out.println("Model ::::::::::::: " + disk.getModel());
                System.out.println("Disk Size (bytes) ::::::::::::: " + disk.getSize());
                System.out.println("Write Bytes ::::::::::::: " + disk.getWriteBytes());
                System.out.println("Writes ::::::::::::: " + disk.getWrites());
                System.out.println("Current Queue Length ::::::::::::: " + disk.getCurrentQueueLength());
                System.out.println("Partitions ::::::::::::: " + disk.getPartitions());
                System.out.println("Read Bytes ::::::::::::: " + disk.getReadBytes());
                System.out.println("Reads ::::::::::::: " + disk.getReads());
                System.out.println("Timestamp ::::::::::::: " + disk.getTimeStamp());
                System.out.println("Transfer Time ::::::::::::: " + disk.getTransferTime());
                System.out.println("Class Information ::::::::::::: " + disk.getClass());


                long writeBytes = disk.getWriteBytes() / 1_000_000_000L;
                System.out.println(writeBytes);
                long roundedToThousands = (writeBytes / 1000) * 1000; // Redondeamos al valor más cercano en miles
                System.out.println("==================" + disk.getModel() + " ************* + " + roundedToThousands);
                System.out.println("OSHI Write Bytes: " + disk.getWriteBytes() + " Writtes: " + disk.getWrites());

                return getTBWFromSMART("/dev/sda");
            }
        }
        throw new IllegalArgumentException("No disk found with specified model: " + diskModel);
    }

    /**
     * Retrieves a list of available SSDs in the system.
     * Filters storage devices to include only those identified as SSDs.
     *
     * @return List of SSD model names
     */
    @Override
    public List<String> getAvailableSSDs() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<String> ssdModels = new ArrayList<>();

        for (HWDiskStore disk : hal.getDiskStores()) {
            if (disk.getModel() != null && disk.getModel().toLowerCase().contains("ssd")) {
                ssdModels.add(disk.getModel());
            }
        }
        return ssdModels;
    }

    /**
     * Retrieves the capacity of a specified disk model in gigabytes.
     *
     * @param diskModel The model name of the disk to query
     * @return Disk capacity in GB
     * @throws IllegalArgumentException if no disk matches the specified model
     */
    @Override
    public long getDiskCapacity(String diskModel) {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            if (disk.getModel().equalsIgnoreCase(diskModel)) {
                return disk.getSize() / (1000 * 1000 * 1000); // Convert to GB
            }
        }
        throw new IllegalArgumentException("No disk found with specified model: " + diskModel);
    }

    /**
     * Detects and retrieves information about all SSDs in the system.
     * Builds a comprehensive DTO containing model and capacity information.
     *
     * @return List of SSDResponseDTO containing details of each detected SSD
     */
    @Override
    public List<SSDResponseDTO> detectSSDs() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<HWDiskStore> diskStores = hal.getDiskStores();

        return diskStores.stream()
                .filter(disk -> disk.getModel() != null && disk.getModel().toLowerCase().contains("ssd"))
                .map(disk -> SSDResponseDTO.builder()
                        .model(disk.getModel())
                        .capacityGB(disk.getSize() / (1024 * 1024 * 1024))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves Total Bytes Written using SMART attributes.
     * Executes smartctl command to get SMART data and extracts the Data Units Written value.
     * Converts the raw value to gigabytes using appropriate scaling factors.
     *
     * @param diskName The device name to query (e.g., "/dev/sda")
     * @return Total bytes written in GB, or 0 if the data cannot be retrieved
     */
    public long getTBWFromSMART(String diskName) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "smartctl",
                    "-A",           // Get SMART attributes
                    "-d", "nvme",   // Specify NVMe device type
                    diskName
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("SMART Output: " + line); // Debug output
                    if (line.contains("Data Units Written")) {
                        // Extract value from "Data Units Written" field
                        String value = line.substring(line.indexOf(":") + 1, line.indexOf("[")).trim();
                        value = value.split(" ")[0].replace(",", ""); // Clean the value
                        long dataUnitsWritten = Long.parseLong(value);

                        // Each "Data Unit" equals 512 KB, convert to GB
                        double totalBytesWritten = dataUnitsWritten * 512 * 931.4; // 512 KB = 512 * 1024 bytes
                        long totalGigabytesWritten = (long) totalBytesWritten / (1000 * 1000 * 1000); // Convert to GB

                        return totalGigabytesWritten;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("smartctl failed with exit code: " + exitCode + " for disk: " + diskName);
            }
        } catch (Exception e) {
            System.err.println("Error executing smartctl: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}