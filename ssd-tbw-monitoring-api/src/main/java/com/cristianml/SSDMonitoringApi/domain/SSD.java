package com.cristianml.SSDMonitoringApi.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "ssds")
public class SSD {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String model;

    private Long capacityGB;

    @Column(nullable = false)
    private LocalDateTime registrationDate;

    @OneToMany(mappedBy = "ssd", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TbwRecord> records = new ArrayList<>();

}
