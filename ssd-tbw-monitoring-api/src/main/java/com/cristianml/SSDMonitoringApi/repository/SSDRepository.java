package com.cristianml.SSDMonitoringApi.repository;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SSDRepository extends JpaRepository<SSDEntity, Long> {

    SSDEntity save(SSDEntity ssdEntity);
    SSDEntity findByModel(String model);
    boolean existsByModel(String model);

}
