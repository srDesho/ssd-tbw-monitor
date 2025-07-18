package com.cristianml.SSDMonitoringApi.dataProvider;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class SsdProvider {

    public static Optional<SSDEntity> ssdEntityOptionalMock() {
        return Optional.of(SSDEntity.builder()
                .id(1L)
                .model("Samsung 980 Pro")
                .serial("S65XNJ0R789123")
                .capacityGB(1000L) // 1TB
                .registrationDate(LocalDateTime.of(2025, 1, 15, 10, 30))
                .isMonitored(true)
                .records(null)
                .build());
    }

    public static List<SSDEntity> ssdEntityList() {
        return List.of(
                SSDEntity.builder()
                        .id(1L)
                        .model("Samsung 980 Pro")
                        .serial("S65XNJ0R789123")
                        .capacityGB(1000L) // 1TB
                        .registrationDate(LocalDateTime.of(2025, 1, 15, 10, 30))
                        .isMonitored(true)
                        .records(null)
                        .build(),

                SSDEntity.builder()
                        .id(2L)
                        .model("Crucial MX500")
                        .serial("CT9876543210AB")
                        .capacityGB(500L)
                        .registrationDate(LocalDateTime.of(2025, 1, 20, 14, 15))
                        .isMonitored(true)
                        .records(null)
                        .build(),

                SSDEntity.builder()
                        .id(3L)
                        .model("Western Digital Blue SN570")
                        .serial("WD1234567890CD")
                        .capacityGB(2000L) // 2TB
                        .registrationDate(LocalDateTime.of(2025, 1, 25, 16, 0))
                        .isMonitored(true)
                        .records(null)
                        .build()
                );
    }
}
