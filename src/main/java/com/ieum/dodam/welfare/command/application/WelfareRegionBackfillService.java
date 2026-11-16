package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 과거 사용자 프로필의 지역 명칭을 정규화한다. 사업 지역은 동기화 시 전용 지역 테이블에 저장한다.
 */
@Service
public class WelfareRegionBackfillService {
    private static final Logger log = LoggerFactory.getLogger(WelfareRegionBackfillService.class);
    private final ChildWelfareProfileRepository profiles;

    public WelfareRegionBackfillService(ChildWelfareProfileRepository profiles) {
        this.profiles = profiles;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        try {
            int updated = backfill();
            if (updated > 0) log.info("복지 프로필 지역 정규화 보정 완료: {}건", updated);
        } catch (Exception exception) {
            log.warn("복지 프로필 지역 정규화 보정 실패", exception);
        }
    }

    @Transactional
    public int backfill() {
        int updated = 0;
        List<ChildWelfareProfile> staleProfiles = profiles.findByRegionProvinceIsNull();
        for (ChildWelfareProfile profile : staleProfiles) {
            if (profile.refreshRegion()) updated++;
        }
        profiles.saveAll(staleProfiles);
        return updated;
    }
}
