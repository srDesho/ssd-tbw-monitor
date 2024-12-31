package com.cristianml.SSDMonitoringApi.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TbwRecordRequestDTO {
    private Long ssdId;
    private LocalDate date;
    private LocalTime time;
    private Long tbw;
}
