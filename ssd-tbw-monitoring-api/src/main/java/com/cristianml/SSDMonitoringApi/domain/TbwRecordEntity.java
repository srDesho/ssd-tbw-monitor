package com.cristianml.SSDMonitoringApi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Long tbw; // in GB

}
