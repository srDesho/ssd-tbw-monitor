package com.cristianml.SSDMonitoringApi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TbwRecordResponseDTO {
    private Long ssdId;
    private LocalDate date;
    private LocalTime time;
    private Long tbw;

}

