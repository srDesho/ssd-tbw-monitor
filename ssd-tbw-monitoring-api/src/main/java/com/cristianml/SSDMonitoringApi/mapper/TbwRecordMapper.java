package com.cristianml.SSDMonitoringApi.mapper;

import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TbwRecordMapper {

    private final ModelMapper modelMapper;

    public TbwRecordResponseDTO toResponseDTO(TbwRecordEntity tbwRecord) {
        return modelMapper.map(tbwRecord, TbwRecordResponseDTO.class);
    }

    public List<TbwRecordResponseDTO> tbwRecordResponseDTOList(List<TbwRecordEntity> tbwRecordList) {
        return tbwRecordList.stream()
                .map(this::toResponseDTO)
                .toList();
    }

}
