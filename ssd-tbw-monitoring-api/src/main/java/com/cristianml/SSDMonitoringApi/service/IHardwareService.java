package com.cristianml.SSDMonitoringApi.service;

import java.util.List;

public interface IHardwareService {

    long getTBW(String diskModel);
    List<String> getAvailableSSDs();
    long getDiskCapacity(String diskModel);

}
