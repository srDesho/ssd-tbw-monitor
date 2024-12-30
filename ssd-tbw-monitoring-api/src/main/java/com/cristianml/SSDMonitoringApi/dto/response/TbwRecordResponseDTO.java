package com.cristianml.SSDMonitoringApi.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TbwRecordResponseDTO {
    private Long ssdId;
    private LocalDate date;
    private LocalTime time;
    private Long tbw;
}

