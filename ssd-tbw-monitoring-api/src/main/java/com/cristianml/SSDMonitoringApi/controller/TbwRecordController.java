package com.cristianml.SSDMonitoringApi.controller;


import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.service.ITbwRecord;
import com.cristianml.SSDMonitoringApi.service.impl.TbwRecordServiceImpl;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tbw-records")
public class TbwRecordController {

    private final ITbwRecord tbwRecordService;

    @GetMapping
    public ResponseEntity<List<TbwRecordResponseDTO>> getAll() {
        List<TbwRecordResponseDTO> tbwRecordEntityList = this.tbwRecordService.findAll();
        return ResponseEntity.ok(tbwRecordEntityList);
    }

    @PostMapping("/auto")
    public ResponseEntity<Void> triggerAutoRegister() {
        this.tbwRecordService.autoRegisterTBW();
        return ResponseEntity.ok().build();
    }
}
