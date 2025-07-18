package com.cristianml.SSDMonitoringApi.domain;

import com.cristianml.SSDMonitoringApi.config.LocalDateConverter;
import com.cristianml.SSDMonitoringApi.config.LocalTimeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "tbw_records", uniqueConstraints = @UniqueConstraint(columnNames = {"date", "ssd_id"}))
public class TbwRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ssd_id", nullable = false)
    private SSDEntity ssd;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Convert(converter = LocalDateConverter.class)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Convert(converter = LocalTimeConverter.class)
    private LocalTime time;

    @Column(nullable = false)
    private Long tbw; // in GB

}
