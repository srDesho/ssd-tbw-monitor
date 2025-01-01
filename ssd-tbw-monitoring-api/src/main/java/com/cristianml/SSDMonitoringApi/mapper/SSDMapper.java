package com.cristianml.SSDMonitoringApi.mapper;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.service.impl.HardwareServiceImpl;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SSDMapper {

    private final ModelMapper modelMapper;

    public SSDResponseDTO toResponseDTO(SSDEntity ssdEntity) {
        SSDResponseDTO ssdResponseDTO = modelMapper.map(ssdEntity, SSDResponseDTO.class);
        ssdResponseDTO.setFormattedDateTime(Utilities.formatLocalDateTime(ssdEntity.getRegistrationDate()));
        System.out.println("###################################" + ssdResponseDTO.getFormattedDateTime());
        return ssdResponseDTO;
    }

    public List<SSDResponseDTO> toSSDResponseDTOList(List<SSDEntity> ssdList) {
        return ssdList.stream()
                .map(this::toResponseDTO)
                .toList();
    }

}
