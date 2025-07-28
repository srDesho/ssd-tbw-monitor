package com.cristianml.SSDMonitoringApi.service.impl;

import com.cristianml.SSDMonitoringApi.dataProvider.SsdProvider;
import com.cristianml.SSDMonitoringApi.dataProvider.TbwRecordProvider;
import com.cristianml.SSDMonitoringApi.domain.SSDEntity;
import com.cristianml.SSDMonitoringApi.domain.TbwRecordEntity;
import com.cristianml.SSDMonitoringApi.dto.response.TbwRecordResponseDTO;
import com.cristianml.SSDMonitoringApi.mapper.TbwRecordMapper;
import com.cristianml.SSDMonitoringApi.repository.SSDRepository;
import com.cristianml.SSDMonitoringApi.repository.TbwRecordRepository;
import com.cristianml.SSDMonitoringApi.service.IHardwareService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TbwRecordServiceImplTest {

    @Mock
    TbwRecordRepository tbwRecordRepository;
    @Mock
    SSDRepository ssdRepository;
    @Mock
    IHardwareService hardwareService;
    @Mock
    TbwRecordMapper tbwRecordMapper;
    @InjectMocks
    TbwRecordServiceImpl tbwRecordService;

    private static final long TBW_UPDATE_THRESHOLD_IN_TEST = 3L;

    // Test for findAll method
    @Test
    public void testFindAll() {
        // Arrange - Test data preparation
        List<TbwRecordEntity> tbwRecordEntities = TbwRecordProvider.tbwRecordEntityList();
        List<TbwRecordResponseDTO> tbwRecordDTOs = TbwRecordProvider.tbwRecordResponseDTOList();

        // Mock behavior configuration
        when(tbwRecordRepository.findAll()).thenReturn(tbwRecordEntities);
        when(tbwRecordMapper.toTbwRecordResponseDTOList(tbwRecordEntities)).thenReturn(tbwRecordDTOs);

        // Act - Execute service method
        List<TbwRecordResponseDTO> result = tbwRecordService.findAll();

        // Assert - Verify results
        verify(tbwRecordRepository, times(1)).findAll();
        verify(tbwRecordMapper, times(1)).toTbwRecordResponseDTOList(tbwRecordEntities);
        assertEquals(tbwRecordDTOs.size(), result.size());
        assertEquals(1L, result.get(0).getSsdId());
    }

    @Test
    public void testAutoRegisterTBW_RegisterNewRecords() {
        // Arrange - Test data for new record registration
        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        Optional<TbwRecordEntity> emptyRecord = Optional.empty(); // No existing record
        List<SSDEntity> ssdEntities = SsdProvider.ssdEntityList();

        // Mock behavior configuration
        when(tbwRecordRepository.findTopByOrderByDateDesc()).thenReturn(Optional.empty()); // No previous records
        when(ssdRepository.findByIsMonitored(true)).thenReturn(ssdEntities);
        when(tbwRecordRepository.findBySsdAndDate(any(SSDEntity.class), eq(currentDate))).thenReturn(emptyRecord);
        when(tbwRecordRepository.save(any(TbwRecordEntity.class))).thenReturn(new TbwRecordEntity());

        // Act - Execute service method
        boolean result = tbwRecordService.autoRegisterTBW();

        // Assert - Verify results
        assertTrue(result); // Expected at least one record registered
        verify(tbwRecordRepository, times(ssdEntities.size())).save(any(TbwRecordEntity.class)); // Save called for each SSD
    }

    @Test
    public void testAutoRegisterTBW_DateManipulated() {
        // Arrange - Setup for date manipulation detection test
        LocalDate currentDate = LocalDate.now();
        LocalDate futureDate = currentDate.plusDays(5); // Future date

        TbwRecordEntity futureRecord = new TbwRecordEntity();
        futureRecord.setDate(futureDate);

        when(tbwRecordRepository.findTopByOrderByDateDesc()).thenReturn(Optional.of(futureRecord));

        // Act - Execute service method
        boolean result = tbwRecordService.autoRegisterTBW();

        // Assert - Verify results
        assertFalse(result); // No registration should occur
        verify(ssdRepository, never()).findByIsMonitored(anyBoolean()); // Should not proceed to find SSDs
    }

    @Test
    public void testAutoRegisterTBW_NoDateManipulation() {
        // Arrange - Setup for valid date scenario
        LocalDate currentDate = LocalDate.now();
        TbwRecordEntity pastRecord = new TbwRecordEntity();
        pastRecord.setDate(currentDate.minusDays(1)); // Past date

        when(tbwRecordRepository.findTopByOrderByDateDesc()).thenReturn(Optional.of(pastRecord));
        when(ssdRepository.findByIsMonitored(true)).thenReturn(SsdProvider.ssdEntityList());
        when(tbwRecordRepository.findBySsdAndDate(any(SSDEntity.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(tbwRecordRepository.save(any(TbwRecordEntity.class))).thenReturn(new TbwRecordEntity());

        // Act - Execute service method
        boolean result = tbwRecordService.autoRegisterTBW();

        // Assert - Verify results
        assertTrue(result); // Registration should occur
        verify(ssdRepository, times(1)).findByIsMonitored(true); // Should proceed to find SSDs
        verify(tbwRecordRepository, atLeastOnce()).save(any(TbwRecordEntity.class)); // Should save records
    }

    @Test
    public void testAutoRegisterTBW_MixedRecordExistence() {
        // Arrange - Setup for mixed record scenario (some exist, some don't)
        LocalDate currentDate = LocalDate.now();
        List<SSDEntity> ssdEntities = SsdProvider.ssdEntityList();

        SSDEntity ssd1 = ssdEntities.get(0);
        SSDEntity ssd2 = ssdEntities.get(1);

        when(tbwRecordRepository.findTopByOrderByDateDesc()).thenReturn(Optional.empty());
        when(ssdRepository.findByIsMonitored(true)).thenReturn(ssdEntities);

        // First SSD already has a record
        when(tbwRecordRepository.findBySsdAndDate(eq(ssd1), eq(currentDate)))
                .thenReturn(Optional.of(new TbwRecordEntity()));

        // Second SSD doesn't have a record
        when(tbwRecordRepository.findBySsdAndDate(eq(ssd2), eq(currentDate)))
                .thenReturn(Optional.empty());

        when(tbwRecordRepository.save(any(TbwRecordEntity.class))).thenReturn(new TbwRecordEntity());

        // Act - Execute service method
        boolean result = tbwRecordService.autoRegisterTBW();

        // Assert - Verify results
        assertTrue(result); // At least one new record registered
        verify(tbwRecordRepository, times(2)).save(any(TbwRecordEntity.class)); // Save called once
    }

    @Test
    public void testAutoRegisterTBW_AllRecordsExist() {
        // Arrange - Setup for scenario where all records exist
        LocalDate currentDate = LocalDate.now();
        List<SSDEntity> ssdEntities = SsdProvider.ssdEntityList();

        when(tbwRecordRepository.findTopByOrderByDateDesc()).thenReturn(Optional.empty());
        when(ssdRepository.findByIsMonitored(true)).thenReturn(ssdEntities);

        // All SSDs already have records
        when(tbwRecordRepository.findBySsdAndDate(any(SSDEntity.class), eq(currentDate)))
                .thenReturn(Optional.of(new TbwRecordEntity()));

        // Act - Execute service method
        boolean result = tbwRecordService.autoRegisterTBW();

        // Assert - Verify results
        assertFalse(result); // No new records registered
        verify(tbwRecordRepository, never()).save(any(TbwRecordEntity.class)); // Save never called
    }

    @Test
    public void testAutoRegisterTBW_GracefulFailure_Simple() {
        // Arrange - Setup graceful failure scenario (one SSD fails, others succeed)
        LocalDate currentDate = LocalDate.now();
        List<SSDEntity> ssdEntities = SsdProvider.ssdEntityList();

        // No previous records exist
        when(tbwRecordRepository.findTopByOrderByDateDesc())
                .thenReturn(Optional.empty());

        // Mock repository to return monitored SSDs
        when(ssdRepository.findByIsMonitored(true))
                .thenReturn(ssdEntities);

        // Create spy to partially mock the service and control processSsdRegistration behavior
        TbwRecordServiceImpl spyService = spy(tbwRecordService);

        // First SSD: Simulate failure during processing
        doThrow(new RuntimeException("Error en SSD 1"))
                .when(spyService)
                .processSsdRegistration(eq(ssdEntities.get(0)), eq(currentDate), any(LocalTime.class));

        // Second SSD: Simulate successful registration
        doReturn(true)
                .when(spyService)
                .processSsdRegistration(eq(ssdEntities.get(1)), eq(currentDate), any(LocalTime.class));

        // Third SSD: Simulate successful registration
        doReturn(true)
                .when(spyService)
                .processSsdRegistration(eq(ssdEntities.get(2)), eq(currentDate), any(LocalTime.class));

        // Act - Execute the auto-registration process
        boolean result = spyService.autoRegisterTBW();

        // Assert - Verify graceful failure behavior
        assertTrue(result); // Returns true because at least one SSD was successfully registered
        verify(spyService, times(3))
                .processSsdRegistration(any(SSDEntity.class), eq(currentDate), any(LocalTime.class));
    }


    @Test
    public void testGetCurrentTbwForSSD_SSDExists() {
        Optional<SSDEntity> ssdEntityOptional = SsdProvider.ssdEntityOptionalMock();
        Long ssdId = ssdEntityOptional.get().getId();

        when(this.ssdRepository.findById(ssdId)).thenReturn(ssdEntityOptional);
        when(this.hardwareService.getTBWFromSMART(anyString())).thenReturn(8051L);

        long result = this.tbwRecordService.getCurrentTbwForSSD(ssdId);

        verify(this.ssdRepository).findById(anyLong());
        verify(this.hardwareService).getTBWFromSMART(anyString());
        assertEquals(8051L, result);
    }

    @Test
    public void testGetCurrentTbwForSSD_SSDDoesNotExist() {
        // Arrange: Setup the test scenario
        Optional<SSDEntity> emptyOptional = Optional.empty();

        when(this.ssdRepository.findById(anyLong())).thenReturn(emptyOptional);
        assertThrows(IllegalArgumentException.class, () -> {
            this.tbwRecordService.getCurrentTbwForSSD(123L); // Using a concrete ID for clarity in the test
        });
    }

    // method checkAndUpdateTbwRecords()

    @Test
    void shouldUpdateTbwRecord_WhenCurrentTbwIsGreaterThanRecordedByThreshold() {
        // Arrange
        LocalDate currentDate = LocalDate.of(2025, 4, 4);
        SSDEntity ssd = SsdProvider.ssdEntityOptionalMock().get();

        long recordedTbw = 1500L * 1024 * 1024 * 1024;
        long currentTbw = 1504L * 1024 * 1024 * 1024; // +4GB → supera umbral

        TbwRecordEntity existingRecord = new TbwRecordEntity();
        existingRecord.setId(1L);
        existingRecord.setDate(currentDate);
        existingRecord.setTime(LocalTime.now());
        existingRecord.setTbw(recordedTbw);

        when(ssdRepository.findByIsMonitored(true)).thenReturn(List.of(ssd));
        when(tbwRecordRepository.findBySsdAndDate(ssd, currentDate)).thenReturn(Optional.of(existingRecord));
        when(hardwareService.getTBWFromSMART(ssd.getModel())).thenReturn(currentTbw);

        // Act
        tbwRecordService.checkAndUpdateTbwRecords(currentDate);

        // Assert
        verify(tbwRecordRepository).save(argThat(record ->
                record.getTbw() == currentTbw && record.getId().equals(existingRecord.getId())
        ));
    }


}