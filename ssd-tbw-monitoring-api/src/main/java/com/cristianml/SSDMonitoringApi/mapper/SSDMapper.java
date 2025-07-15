package com.cristianml.SSDMonitoringApi.mapper;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.service.impl.HardwareServiceImpl;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SSDMapper {

    private final ModelMapper modelMapper;

    public SSDResponseDTO toResponseDTO(SSDEntity ssdEntity) {
        SSDResponseDTO ssdResponseDTO = modelMapper.map(ssdEntity, SSDResponseDTO.class);
        ssdResponseDTO.setFormattedDateTime(Utilities.formatLocalDateTime(ssdEntity.getRegistrationDate()));

        if (ssdEntity.getRecords() != null && !ssdEntity.getRecords().isEmpty()) {
            List<TbwRecordResponseDTO> recordDTOs = ssdEntity.getRecords().stream()
                    .map(this::mapTbwRecordToDTO)
                    .collect(Collectors.toList());
            ssdResponseDTO.setRecords(recordDTOs);
        }

        return ssdResponseDTO;
    }

    private TbwRecordResponseDTO mapTbwRecordToDTO(TbwRecordEntity record) {
        return TbwRecordResponseDTO.builder()
                .ssdId(record.getSsd().getId())
                .date(record.getDate())
                .time(record.getTime())
                .tbw(record.getTbw())
                .build();
    }

    public List<SSDResponseDTO> toSSDResponseDTOList(List<SSDEntity> ssdList) {
        return ssdList.stream()
                .map(this::toResponseDTO)
                .toList();
    }

}
