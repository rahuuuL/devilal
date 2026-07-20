package com.terminal_devilal.core_processes.sync_data.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PdvPersistenceServiceTest {

    @Test
    void parsesLegacyAndIsoTimestampValues() {
        PdvPersistenceService service = new PdvPersistenceService(null, null, null, null, null);

        assertEquals(LocalDate.of(2021, 3, 30), service.parseTimestamp("2021-03-30T18:30:00.000Z"));
        assertEquals(LocalDate.of(2021, 3, 30), service.parseTimestamp("30-Mar-2021"));
    }
}
