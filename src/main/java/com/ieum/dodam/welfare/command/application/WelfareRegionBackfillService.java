package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.WelfareRegionScope;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지역 정규화 컬럼이 비어 있는 기존 데이터를 소관기관 정보로 재판정한다.
 *
 * <p>지역 계층 컬럼을 추가한 마이그레이션은 값을 UNKNOWN으로 남겨두므로, 동기화를 다시 돌리지
 * 않아도 추천이 올바르게 동작하도록 기동 직후 한 번 보정한다.
 */
@Service
public class WelfareRegionBackfillService {
    private static final Logger log = LoggerFactory.getLogger(WelfareRegionBackfillService.class);
    private final WelfareProgramRepository programs;
    private final ChildWelfareProfileRepository profiles;

    public WelfareRegionBackfillService(WelfareProgramRepository programs, ChildWelfareProfileRepository profiles) {
        this.programs = programs;
        this.profiles = profiles;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        try {
            int updated = backfill();
            if (updated > 0) log.info("복지 사업 지역 정규화 보정 완료: {}건", updated);
        } catch (Exception exception) {
            log.warn("복지 사업 지역 정규화 보정 실패", exception);
        }
    }

    @Transactional
    public int backfill() {
        List<WelfareProgram> pending = programs.findByRegionScope(WelfareRegionScope.UNKNOWN);
        int updated = 0;
        for (WelfareProgram program : pending) {
            if (program.refreshRegion()) updated++;
        }
        programs.saveAll(pending);
        List<ChildWelfareProfile> staleProfiles = profiles.findByRegionProvinceIsNull();
        for (ChildWelfareProfile profile : staleProfiles) {
            if (profile.refreshRegion()) updated++;
        }
        profiles.saveAll(staleProfiles);
        return updated;
    }
}
