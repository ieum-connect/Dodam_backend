package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.entity.WelfareProgramRaw;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRawRepository;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 외부 서비스ID 기준으로 카탈로그를 생성·갱신·비활성화한다.
 *
 * <p>한 번의 upsert가 세 곳을 함께 맞춘다. 정규화 필드는 {@code welfare_programs}, 응답 원문은
 * {@code welfare_program_raw}, 제공 지역은 {@code welfare_program_regions}다. 같은 트랜잭션에서
 * 처리하므로 원문만 남고 사업이 없거나 그 반대인 상태는 생기지 않는다.
 */
@Service
public class WelfareCatalogService {
    private final WelfareProgramRepository programs;
    /** 정부24 원문 전용 저장소. */
    private final WelfareProgramRawRepository rawPayloads;
    private final WelfareProgramRegionService regions;
    private final Clock clock;

    @Autowired
    public WelfareCatalogService(WelfareProgramRepository programs, WelfareProgramRawRepository rawPayloads,
                                 WelfareProgramRegionService regions) {
        this(programs, rawPayloads, regions, Clock.systemUTC());
    }

    /** 단위 테스트에서 정규화 본문만 검증할 때 쓰는 생성자. */
    WelfareCatalogService(WelfareProgramRepository programs, Clock clock) {
        this(programs, null, null, clock);
    }

    WelfareCatalogService(WelfareProgramRepository programs, WelfareProgramRawRepository rawPayloads,
                          WelfareProgramRegionService regions, Clock clock) {
        this.programs = programs;
        this.rawPayloads = rawPayloads;
        this.regions = regions;
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
        WelfareProgram saved = programs.save(program);
        storeRawPayload(saved.getId(), source, now);
        if (regions != null) regions.sync(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean needsEnrichment(WelfareProgramSource listSource) {
        if (rawPayloads == null) return true;
        return programs.findByExternalServiceId(listSource.externalServiceId())
                .flatMap(program -> rawPayloads.findById(program.getId()))
                .map(raw -> raw.requiresEnrichment(listSource.rawListJson()))
                .orElse(true);
    }

    @Transactional
    public void deactivate(UUID programId) {
        WelfareProgram program = programs.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("복지 서비스를 찾을 수 없습니다."));
        program.deactivate(clock.instant());
    }

    private void storeRawPayload(UUID programId, WelfareProgramSource source, Instant now) {
        if (rawPayloads == null) return;
        WelfareProgramRaw raw = rawPayloads.findById(programId)
                .map(existing -> {
                    existing.update(source.rawListJson(), source.rawDetailJson(), source.rawConditionJson(), now);
                    return existing;
                })
                .orElseGet(() -> WelfareProgramRaw.of(programId, source.rawListJson(), source.rawDetailJson(),
                        source.rawConditionJson(), now));
        rawPayloads.save(raw);
    }
}
