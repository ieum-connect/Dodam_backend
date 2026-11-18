package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.WelfareRegion;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.entity.WelfareProgramRegion;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 사업의 제공 지역을 {@code welfare_program_regions}에 맞춘다.
 *
 * <p>지역 판정 자체는 {@link com.ieum.dodam.welfare.domain.WelfareRegionResolver}가 하고, 이 서비스는
 * 판정 결과를 테이블에 반영하는 일만 맡는다. 현재 리졸버는 사업당 한 지역을 돌려주지만 테이블과 이
 * 서비스는 여러 지역을 그대로 받으므로, 다중 제공 지역을 판정하게 되더라도 스키마 변경이 필요 없다.
 */
@Service
public class WelfareProgramRegionService {
    private final WelfareProgramRegionRepository regions;
    private final Clock clock;

    // 생성자가 둘이므로 Spring이 주입에 쓸 생성자를 명시한다(다른 하나는 테스트용 Clock 주입).
    @Autowired
    public WelfareProgramRegionService(WelfareProgramRegionRepository regions) {
        this(regions, Clock.systemUTC());
    }

    WelfareProgramRegionService(WelfareProgramRegionRepository regions, Clock clock) {
        this.regions = regions;
        this.clock = clock;
    }

    @Transactional
    public void sync(WelfareProgram program) {
        sync(program.getId(), List.of(program.region()));
    }

    /** 사업의 제공 지역을 주어진 목록과 똑같이 맞춘다. 바뀐 것이 없으면 아무것도 쓰지 않는다. */
    @Transactional
    public void sync(UUID programId, List<WelfareRegion> desired) {
        Instant now = clock.instant();
        Map<String, WelfareRegion> wanted = new LinkedHashMap<>();
        for (WelfareRegion region : desired) {
            WelfareRegion normalized = WelfareProgramRegion.normalize(region);
            wanted.putIfAbsent(WelfareProgramRegion.key(normalized), normalized);
        }
        if (wanted.isEmpty()) wanted.put(WelfareProgramRegion.key(WelfareRegion.UNKNOWN), WelfareRegion.UNKNOWN);

        List<WelfareProgramRegion> current = regions.findByProgramId(programId);
        Set<String> currentKeys = new LinkedHashSet<>();
        List<WelfareProgramRegion> obsolete = new ArrayList<>();
        for (WelfareProgramRegion row : current) {
            if (wanted.containsKey(row.key()) && currentKeys.add(row.key())) continue;
            obsolete.add(row);
        }
        List<WelfareProgramRegion> added = wanted.entrySet().stream()
                .filter(entry -> !currentKeys.contains(entry.getKey()))
                .map(entry -> WelfareProgramRegion.of(programId, entry.getValue(), now))
                .toList();

        if (!obsolete.isEmpty()) regions.deleteAll(obsolete);
        if (!added.isEmpty()) regions.saveAll(added);
    }

    /**
     * 사업별 제공 지역을 한 번에 읽는다.
     *
     * <p>아직 행이 없는 사업은 결과에 담기지 않는다. 정상 동기화된 사업에는 항상 한 행 이상 있다.
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<WelfareRegion>> regionsOf(Collection<UUID> programIds) {
        if (programIds == null || programIds.isEmpty()) return Map.of();
        Map<UUID, List<WelfareRegion>> result = new HashMap<>();
        for (WelfareProgramRegion row : regions.findByProgramIdIn(programIds)) {
            result.computeIfAbsent(row.getProgramId(), key -> new ArrayList<>()).add(row.toRegion());
        }
        return result;
    }
}
