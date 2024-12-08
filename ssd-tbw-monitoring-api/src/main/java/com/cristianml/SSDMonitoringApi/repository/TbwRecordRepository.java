package com.cristianml.SSDMonitoringApi.repository;

import com.cristianml.SSDMonitoringApi.domain.TbwRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface TbwRecordRepository extends JpaRepository<TbwRecord, Long> {

    boolean existsByDateAndSsd_id(LocalDate date, Long ssdId);

}
