package com.cristianml.SSDMonitoringApi.dataProvider;

import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class TbwRecordProvider {

    public static Optional<TbwRecordEntity> tbwRecordEntityMock() {
        return Optional.of(TbwRecordEntity.builder()
                .id(1L)
                .date(LocalDate.of(2025, 01, 25))
                .time(LocalTime.of(17, 10))
                .tbw(1500L)
                .build());
    }

    public static Optional<TbwRecordEntity> higherDateRecordOptMock() {
        return Optional.of(TbwRecordEntity.builder()
                .id(1L)
                .date(LocalDate.of(2035, 01, 25))
                .time(LocalTime.of(17, 10))
                .tbw(1500L)
                .build());
    }

    public static Optional<TbwRecordEntity> tbwRecordEntityCurrentDateMock() {
        return Optional.of(TbwRecordEntity.builder()
                .id(1L)
                .date(LocalDate.now())
                .time(LocalTime.of(17, 10))
                .tbw(1500L)
                .build());
    }

    public static List<TbwRecordEntity> tbwRecordEntityList() {
        return List.of(
                TbwRecordEntity.builder()
                        .id(1L)
                        .date(LocalDate.of(2025, 01, 25))
                        .time(LocalTime.of(17, 10))
                        .tbw(1500L)
                        .build(),
                TbwRecordEntity.builder()
                        .id(2L)
                        .date(LocalDate.of(2025, 01, 26))
                        .time(LocalTime.of(18, 11))
                        .tbw(1510L)
                        .build(),
                TbwRecordEntity.builder()
                        .id(3L)
                        .date(LocalDate.of(2025, 01, 27))
                        .time(LocalTime.of(19, 50))
                        .tbw(1525L)
                        .build()
        );
    }

    public static List<TbwRecordResponseDTO> tbwRecordResponseDTOList() {
        return List.of(
                TbwRecordResponseDTO.builder()
                        .ssdId(1L)
                        .date(LocalDate.of(2025, 01, 25))
                        .time(LocalTime.of(17, 10))
                        .tbw(1500L)
                        .build(),
                TbwRecordResponseDTO.builder()
                        .ssdId(2L)
                        .date(LocalDate.of(2025, 01, 26))
                        .time(LocalTime.of(18, 11))
                        .tbw(1510L)
                        .build(),
                TbwRecordResponseDTO.builder()
                        .ssdId(3L)
                        .date(LocalDate.of(2025, 01, 25))
                        .time(LocalTime.of(19, 50))
                        .tbw(1525L)
                        .build()
        );
    }

}
