package com.cristianml.SSDMonitoringApi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SSDResponseDTO {

    private Long id;
    private String model;
    private String serial;
    private Long capacityGB;
    private LocalDateTime registrationDate;
    private Boolean isMonitored;
    private String formattedDateTime;

    private List<TbwRecordResponseDTO> records;
}
