package com.cristianml.SSDMonitoringApi.repository;

import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface TbwRecordRepository extends JpaRepository<TbwRecordEntity, Long> {

    boolean existsByDateAndSsd_id(LocalDate date, Long ssdId);

}
