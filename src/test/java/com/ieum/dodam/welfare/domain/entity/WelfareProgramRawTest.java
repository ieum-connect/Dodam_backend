package com.ieum.dodam.welfare.domain.entity;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WelfareProgramRawTest {
    private static final Instant NOW = Instant.parse("2026-11-17T00:00:00Z");

    @Test
    void marksMissingDetailAndConditionForEnrichment() {
        var list = JsonNodeFactory.instance.objectNode().put("서비스ID", "SVC-1");
        WelfareProgramRaw raw = WelfareProgramRaw.of(UUID.randomUUID(), list, null, null, NOW);

        assertThat(raw.getDetailJson()).isEmpty();
        assertThat(raw.getConditionJson()).isEmpty();
        assertThat(raw.requiresEnrichment(list)).isTrue();
    }

    @Test
    void listOnlyUpdatePreservesExistingDetailAndConditions() {
        var oldList = JsonNodeFactory.instance.objectNode().put("version", 1);
        var newList = JsonNodeFactory.instance.objectNode().put("version", 2);
        var detail = JsonNodeFactory.instance.objectNode().put("purpose", "child care");
        var conditions = JsonNodeFactory.instance.arrayNode().addObject().put("JA", "value");
        WelfareProgramRaw raw = WelfareProgramRaw.of(UUID.randomUUID(), oldList, detail, conditions, NOW);

        raw.update(newList, null, null, NOW.plusSeconds(60));

        assertThat(raw.getListJson()).isEqualTo(newList);
        assertThat(raw.getDetailJson()).isEqualTo(detail);
        assertThat(raw.getConditionJson()).isEqualTo(conditions);
    }

    @Test
    void hashReflectsPreservedDetailAfterListOnlyUpdate() {
        var oldList = JsonNodeFactory.instance.objectNode().put("version", 1);
        var newList = JsonNodeFactory.instance.objectNode().put("version", 2);
        var detail = JsonNodeFactory.instance.objectNode().put("purpose", "child care");
        var conditions = JsonNodeFactory.instance.arrayNode().addObject().put("JA", "value");
        WelfareProgramRaw raw = WelfareProgramRaw.of(UUID.randomUUID(), oldList, detail, conditions, NOW);
        WelfareProgramRaw expected = WelfareProgramRaw.of(UUID.randomUUID(), newList, detail, conditions, NOW);

        raw.update(newList, null, null, NOW.plusSeconds(60));

        assertThat(raw.getPayloadHash()).isEqualTo(expected.getPayloadHash());
    }

    @Test
    void repeatedListOnlyUpdateReportsNoChange() {
        var list = JsonNodeFactory.instance.objectNode().put("version", 1);
        var detail = JsonNodeFactory.instance.objectNode().put("purpose", "child care");
        var conditions = JsonNodeFactory.instance.arrayNode().addObject().put("JA", "value");
        WelfareProgramRaw raw = WelfareProgramRaw.of(UUID.randomUUID(), list, detail, conditions, NOW);

        assertThat(raw.update(list, null, null, NOW.plusSeconds(60))).isFalse();
    }
}
