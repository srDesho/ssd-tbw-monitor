package com.cristianml.SSDMonitoringApi.controller;


import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.service.ITbwRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
