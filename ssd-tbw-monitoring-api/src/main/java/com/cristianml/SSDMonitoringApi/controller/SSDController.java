package com.cristianml.SSDMonitoringApi.controller;

import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.dto.request.SSDRequestDTO;
import com.cristianml.SSDMonitoringApi.dto.response.SSDResponseDTO;
import com.cristianml.SSDMonitoringApi.service.impl.SSDServiceImpl;
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

    @GetMapping
    public ResponseEntity<List<SSDEntity>> getAllSSDs() {
        List<SSDEntity> ssdEntityList = this.ssdService.findAll();
        return ResponseEntity.ok(ssdEntityList);
    }

    @PostMapping
    public ResponseEntity<Object> registerSSD(@RequestBody SSDRequestDTO request) {
        SSDResponseDTO ssdResponseDTO = this.ssdService.registerSsd(request.getModel(), request.getCapacityGB());
        return Utilities.generateResponse(HttpStatus.CREATED, "SSD created successfully.");
    }
}
