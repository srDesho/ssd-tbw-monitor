package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// This class provides methods to interact with hardware information such as retrieving TBW (Total Bytes Written), disk capacity, and available SSD models.
// The OSHI library is used to extract hardware details from the system.

@Service
public class HardwareServiceImpl implements IHardwareService {

    // Retrieves the Total Bytes Written (TBW) for a specified disk model, converting the value from bytes to gigabytes (GB).
    @Override
    public long getTBW(String diskModel) {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            if (disk.getModel().equalsIgnoreCase(diskModel)) {
                return disk.getWriteBytes() / (1024 * 1024 * 1024); // Convert to GB
            }
        }
        throw new IllegalArgumentException("No disk found with specified model: " + diskModel);
    }

    // Retrieves a list of available SSDs on the system by checking disk models that include "SSD".
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

    // Retrieves the capacity of a specified disk model in gigabytes (GB), converting the size from bytes to gigabytes.
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

    // Detect SSDs
    @Override
    public List<SSDResponseDTO> detectSSDs() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        List<HWDiskStore> diskStores = hal.getDiskStores();

        // Filter disks that are SSDs
        return diskStores.stream()
                .filter(disk -> disk.getModel() != null && disk.getModel().toLowerCase().contains("ssd"))
                .map(disk -> SSDResponseDTO.builder()
                        .model(disk.getModel())
                        .capacityGB(disk.getSize() / (1024 * 1024 * 1024))
                        .build())
                .collect(Collectors.toList());
    }
}