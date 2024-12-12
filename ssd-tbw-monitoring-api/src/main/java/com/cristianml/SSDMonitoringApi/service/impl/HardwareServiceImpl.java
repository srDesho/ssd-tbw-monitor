package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utility methods to interact with hardware information
 * such as retrieving disk TBW (Total Bytes Written), disk capacity, and available SSD models.
 * It uses the OSHI library to extract hardware details from the system.
 */
public class HardwareServiceImpl implements IHardwareService {

    /**
     * Retrieves the Total Bytes Written (TBW) for a specified disk model.
     * Converts the value from bytes to gigabytes (GB).
     * @param diskModel The model of the disk to retrieve the TBW for.
     * @return The TBW in GB.
     * @throws IllegalArgumentException If no disk with the specified model is found.
     */
    @Override
    public long getTBW(String diskModel) {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            if (disk.getModel().equalsIgnoreCase(diskModel)) {
                // Returns the total number of bytes written to the disk.
                return disk.getWriteBytes() / (1024 * 1024 * 1024); // Convert to GB
            }
        }
        throw new IllegalArgumentException("No disk found with specified model: " + diskModel);
    }

    /**
     * Retrieves a list of available SSDs on the system by checking the disk models for "SSD".
     * @return A list of SSD models.
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
     * Retrieves the capacity of a specified disk model in gigabytes (GB).
     * Converts the size from bytes to gigabytes.
     * @param diskModel The model of the disk to retrieve the capacity for.
     * @return The disk capacity in GB.
     * @throws IllegalArgumentException If no disk with the specified model is found.
     */
    @Override
    public long getDiskCapacity(String diskModel) {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            if (disk.getModel().equalsIgnoreCase(diskModel)) {
                return disk.getSize() / (1024 * 1024 * 1024); // Convert to GB
            }
        }
        throw new IllegalArgumentException("No disk found with specified model: " + diskModel);
    }
}