package com.cristianml.SSDMonitoringApi.repository;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SSDRepository extends JpaRepository<SSDEntity, Long> {

    SSDEntity save(SSDEntity ssdEntity);
    SSDEntity findByModel(String model);
    boolean existsByModelAndSerial(String model, String serial);
    List<SSDEntity> findByIsMonitored(Boolean isMonitored);

    SSDEntity findByModelAndSerial(String model, String serial);
}
