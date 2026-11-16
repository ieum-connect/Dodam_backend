package com.ieum.dodam.welfare.domain.repository;

import com.ieum.dodam.welfare.domain.entity.WelfareProgramRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WelfareProgramRegionRepository extends JpaRepository<WelfareProgramRegion, UUID> {
    List<WelfareProgramRegion> findByProgramId(UUID programId);

    /** 추천 후보 전체의 제공 지역을 한 번에 읽어 사업당 조회가 반복되지 않게 한다. */
    List<WelfareProgramRegion> findByProgramIdIn(Collection<UUID> programIds);
}
