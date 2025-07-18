package com.cristianml.SSDMonitoringApi.dto.request;

import lombok.Data;

@Data
public class SSDRequestDTO {

    private String model;
    private Long capacityGB;

}
