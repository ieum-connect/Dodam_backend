package com.ieum.dodam.welfare.query.application;

import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class WelfareProgramQueryService {
    private final WelfareProgramRepository programs;
    public WelfareProgramQueryService(WelfareProgramRepository programs) { this.programs = programs; }
    public List<WelfareProgram> list(String query, int size) {
        var pageable = PageRequest.of(0, Math.min(Math.max(size, 1), 100));
        return query == null || query.isBlank()
                ? programs.findByActiveTrueOrderByModifiedAtSourceDescIdDesc(pageable)
                : programs.findByActiveTrueAndNameContainingIgnoreCaseOrderByModifiedAtSourceDescIdDesc(query, pageable);
    }
    public WelfareProgram get(UUID id) {
        return programs.findById(id).filter(WelfareProgram::isActive)
                .orElseThrow(() -> new IllegalArgumentException("복지 서비스를 찾을 수 없습니다."));
    }
}
