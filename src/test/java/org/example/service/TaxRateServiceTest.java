package org.example.service;

import org.example.model.TaxRate;
import org.example.repository.TaxRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TaxRateService taxRateService;

    @Test
    void save_SetAsDefault_UnsetsPreviousDefault() {
        TaxRate oldDefault = new TaxRate();
        oldDefault.setId(1L);
        oldDefault.setDefaultRate(true);

        TaxRate newDefault = new TaxRate();
        newDefault.setId(2L);
        newDefault.setDefaultRate(true);

        when(taxRateRepository.findByIsDefaultTrue()).thenReturn(Optional.of(oldDefault));
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(i -> i.getArgument(0));

        taxRateService.save(newDefault);

        assertFalse(oldDefault.isDefaultRate());
        verify(taxRateRepository, times(1)).save(oldDefault);
        verify(taxRateRepository, times(1)).save(newDefault);
    }

    @Test
    void save_TryToUnsetDefault_WhenNoOtherExists_ForcesDefault() {
        TaxRate currentDefault = new TaxRate();
        currentDefault.setId(1L);
        currentDefault.setDefaultRate(false);

        when(taxRateRepository.findByIsDefaultTrue()).thenReturn(Optional.of(currentDefault));
        when(taxRateRepository.save(any(TaxRate.class))).thenAnswer(i -> i.getArgument(0));

        taxRateService.save(currentDefault);

        assertTrue(currentDefault.isDefaultRate(), "Služba musí vynutit defaultRate=true, protože není žádná jiná.");
        verify(taxRateRepository, times(1)).save(currentDefault);
    }

    @Test
    void delete_NonDefaultRate_DeletesSuccessfully() {
        TaxRate rate = new TaxRate();
        rate.setId(1L);
        rate.setDefaultRate(false);

        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(rate));

        taxRateService.delete(1L);

        verify(taxRateRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_DefaultRate_ThrowsException() {
        TaxRate rate = new TaxRate();
        rate.setId(1L);
        rate.setDefaultRate(true);

        when(taxRateRepository.findById(1L)).thenReturn(Optional.of(rate));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            taxRateService.delete(1L);
        });

        assertEquals("Nelze smazat výchozí daňovou sazbu.", exception.getMessage());
        verify(taxRateRepository, never()).deleteById(anyLong());
    }
}