package com.cristianml.SSDMonitoringApi.repository;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TbwRecordRepository extends JpaRepository<TbwRecordEntity, Long> {

    Optional<TbwRecordEntity> findBySsdAndDate(SSDEntity ssd, LocalDate date);

}
