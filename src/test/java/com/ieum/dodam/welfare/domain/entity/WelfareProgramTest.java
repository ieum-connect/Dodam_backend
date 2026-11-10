package com.ieum.dodam.welfare.domain.entity;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ieum.dodam.welfare.domain.WelfareProgramSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class WelfareProgramTest {
    private static final Instant FIRST = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-11-05T00:00:00Z");

    @Test
    void createsActiveProgramWithRawPayloads() {
        WelfareProgram program = WelfareProgram.create(source("처음", FIRST), FIRST);

        assertThat(program.getId()).isNotNull();
        assertThat(program.getExternalServiceId()).isEqualTo("SVC-1");
        assertThat(program.getName()).isEqualTo("처음");
        assertThat(program.isActive()).isTrue();
        assertThat(program.getRawListJson().get("서비스ID").asText()).isEqualTo("SVC-1");
    }

    @Test
    void ignoresOlderSourceButRecordsSyncTime() {
        WelfareProgram program = WelfareProgram.create(source("최신", SECOND), SECOND);

        boolean changed = program.updateFrom(source("과거", FIRST), SECOND.plusSeconds(60));

        assertThat(changed).isFalse();
        assertThat(program.getName()).isEqualTo("최신");
        assertThat(program.getLastSyncedAt()).isEqualTo(SECOND.plusSeconds(60));
    }

    @Test
    void updatesNewerSourceAndReactivatesProgram() {
        WelfareProgram program = WelfareProgram.create(source("처음", FIRST), FIRST);
        program.deactivate(FIRST.plusSeconds(1));

        boolean changed = program.updateFrom(source("변경", SECOND), SECOND);

        assertThat(changed).isTrue();
        assertThat(program.getName()).isEqualTo("변경");
        assertThat(program.isActive()).isTrue();
    }

    public static WelfareProgramSource source(String name, Instant modifiedAt) {
        return new WelfareProgramSource(
                "SVC-1", name, "현금", "요약", "목적", "만 7세 이하", "지역 거주", "지원",
                "온라인", "상시", "상시", "신청서", null, null, "https://apply.example",
                "https://detail.example", "A1", "기관", "중앙", "부서", "주민센터", "02-0000-0000",
                "보육", "영유아", 1L, FIRST, modifiedAt, true,
                JsonNodeFactory.instance.objectNode().put("서비스ID", "SVC-1"),
                JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode());
    }
}
