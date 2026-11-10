package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ieum.dodam.welfare.domain.entity.WelfareProgramTest.source;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WelfareCatalogServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-05T02:00:00Z");
    private final WelfareProgramRepository programs = mock(WelfareProgramRepository.class);
    private final WelfareCatalogService service = new WelfareCatalogService(
            programs, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void insertsWhenExternalServiceIdIsNew() {
        when(programs.findByExternalServiceId("SVC-1")).thenReturn(Optional.empty());
        when(programs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WelfareProgram saved = service.upsert(source("신규", NOW));

        assertThat(saved.getName()).isEqualTo("신규");
        verify(programs).save(saved);
    }

    @Test
    void updatesExistingProgramWithoutCreatingDuplicate() {
        WelfareProgram existing = WelfareProgram.create(source("기존", NOW.minusSeconds(60)), NOW.minusSeconds(60));
        when(programs.findByExternalServiceId("SVC-1")).thenReturn(Optional.of(existing));
        when(programs.save(existing)).thenReturn(existing);

        WelfareProgram saved = service.upsert(source("변경", NOW));

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getName()).isEqualTo("변경");
        verify(programs).save(existing);
    }
}
