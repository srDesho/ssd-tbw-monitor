package com.cristianml.SSDMonitoringApi.mapper;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SSDMapper {

    private final ModelMapper modelMapper;

    public SSDResponseDTO toResponseDTO(SSDEntity ssdEntity) {
        return modelMapper.map(ssdEntity, SSDResponseDTO.class);
    }

    public List<SSDResponseDTO> toSSDResponseDTOList(List<SSDEntity> ssdList) {
        return ssdList.stream()
                .map(this::toResponseDTO)
                .toList();
    }

}
