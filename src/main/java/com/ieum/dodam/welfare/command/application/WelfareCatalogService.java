package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** 외부 서비스ID 기준으로 카탈로그를 생성·갱신·비활성화한다. */
@Service
public class WelfareCatalogService {
    private final WelfareProgramRepository programs;
    private final Clock clock;

    @Autowired
    public WelfareCatalogService(WelfareProgramRepository programs) {
        this(programs, Clock.systemUTC());
    }

    WelfareCatalogService(WelfareProgramRepository programs, Clock clock) {
        this.programs = programs;
        this.clock = clock;
    }

    @Transactional
    public WelfareProgram upsert(WelfareProgramSource source) {
        Instant now = clock.instant();
        WelfareProgram program = programs.findByExternalServiceId(source.externalServiceId())
                .map(existing -> {
                    existing.updateFrom(source, now);
                    return existing;
                })
                .orElseGet(() -> WelfareProgram.create(source, now));
        return programs.save(program);
    }

    @Transactional
    public void deactivate(UUID programId) {
        WelfareProgram program = programs.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("복지 서비스를 찾을 수 없습니다."));
        program.deactivate(clock.instant());
    }
}
