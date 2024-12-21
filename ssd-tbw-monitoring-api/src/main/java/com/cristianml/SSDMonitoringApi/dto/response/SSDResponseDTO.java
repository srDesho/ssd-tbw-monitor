package com.cristianml.SSDMonitoringApi.dto.response;

import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SSDResponseDTO {

    private Long id;
    private String model;
    private Long capacityGB;
    private LocalDateTime registrationDate;

}
