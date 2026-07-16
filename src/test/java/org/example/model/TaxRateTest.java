package org.example.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class TaxRateTest {
    @Test
    void builder_CreatesValidTaxRate() {
        TaxRate rate = TaxRate.builder()
                .name("DPH")
                .rate(new BigDecimal("21"))
                .defaultRate(true)
                .build();

        assertEquals("DPH", rate.getName());
        assertTrue(rate.isDefaultRate());
    }
}