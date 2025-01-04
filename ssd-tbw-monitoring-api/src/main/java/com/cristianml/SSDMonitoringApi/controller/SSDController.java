package com.cristianml.SSDMonitoringApi.controller;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.request.SSDRequestDTO;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.service.impl.HardwareServiceImpl;
import com.cristianml.SSDMonitoringApi.service.impl.SSDServiceImpl;
import com.cristianml.SSDMonitoringApi.service.impl.TbwRecordServiceImpl;
import com.cristianml.SSDMonitoringApi.utilities.Utilities;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ssds")
public class SSDController {

    private final SSDServiceImpl ssdService;
    private final HardwareServiceImpl hardwareService;
    private final TbwRecordServiceImpl tbwRecordService;

    @GetMapping
    public ResponseEntity<List<SSDResponseDTO>> getAllSSDs() {
        List<SSDResponseDTO> ssdEntityList = this.ssdService.findAll();
        return ResponseEntity.ok(ssdEntityList);
    }

    @GetMapping("/detect")
    public ResponseEntity<List<SSDResponseDTO>> detectSSDs() {
        List<SSDResponseDTO> detectedSSDs = this.hardwareService.detectSSDs();
        return ResponseEntity.ok(detectedSSDs);
    }

    @PostMapping("/detect-and-register")
    public ResponseEntity<Object> detectAndRegisterSSD() {
        this.ssdService.detectAndRegisterSsd();
        return Utilities.generateResponse(HttpStatus.OK, "Register Successfully.");
    }

    @PatchMapping("/{id}/monitor")
    public ResponseEntity<Object> toggleMonitoring(@PathVariable long id, @RequestParam("monitor") boolean monitor) {
        this.ssdService.toggleMonitoring(id, monitor);
        return Utilities.generateResponse(HttpStatus.OK, "SSD Monitoring status updated successfully.");
    }

    @GetMapping("/all")
    public ResponseEntity<List<TbwRecordResponseDTO>> all() {
        this.ssdService.detectAndRegisterSsd();
        this.tbwRecordService.autoRegisterTBW();
        List<TbwRecordResponseDTO> tbwRecordResponseDTO = this.tbwRecordService.findAll();
        return ResponseEntity.ok(tbwRecordResponseDTO);
    }
}
