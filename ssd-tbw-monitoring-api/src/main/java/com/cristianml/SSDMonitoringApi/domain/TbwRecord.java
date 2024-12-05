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
@Table(name = "tbw_records", uniqueConstraints = @UniqueConstraint(columnNames = {"fecha", "ssd_id"}))
public class TbwRecord {

    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "ssd_id", nullable = false)
    private SSD ssd;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Long tbw; // in GB

}
