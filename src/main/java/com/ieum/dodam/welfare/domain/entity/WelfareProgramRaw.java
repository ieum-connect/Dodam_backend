package com.ieum.dodam.welfare.domain.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * 정부24 응답 원문. 사업 한 건당 한 행이며 {@link WelfareProgram}과 1:1이다.
 *
 * <p>원문은 재처리의 기준이 되는 불변 데이터다. 파싱 규칙이 바뀌면 외부 API를 다시 호출하지 않고
 * 이 테이블만으로 정규화 필드와 파생 데이터를 다시 만들 수 있어야 한다. 목록 조회에는 쓰이지 않으므로
 * 본문 테이블에서 분리해 {@code welfare_programs}의 행 크기를 줄인다.
 */
@Entity
@Table(name = "welfare_program_raw")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelfareProgramRaw {
    @Id
    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "list_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode listJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode detailJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode conditionJson;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public static WelfareProgramRaw of(UUID programId, JsonNode listJson, JsonNode detailJson,
                                       JsonNode conditionJson, Instant fetchedAt) {
        WelfareProgramRaw raw = new WelfareProgramRaw();
        raw.programId = programId;
        raw.apply(listJson, detailJson, conditionJson, fetchedAt);
        return raw;
    }

    /**
     * 원문을 최신 응답으로 바꾼다. 내용이 실제로 달라졌으면 {@code true}를 돌려주어
     * 재파싱·재색인 대상을 가려낼 수 있게 한다.
     */
    public boolean update(JsonNode listJson, JsonNode detailJson, JsonNode conditionJson, Instant fetchedAt) {
        String previous = payloadHash;
        apply(listJson, detailJson, conditionJson, fetchedAt);
        return previous == null || !previous.equals(payloadHash);
    }

    /** 목록이 바뀌었거나 상세/조건 원문이 아직 없으면 제한 배치 상세 수집 대상이다. */
    public boolean requiresEnrichment(JsonNode incomingListJson) {
        return !java.util.Objects.equals(listJson, normalize(incomingListJson))
                || isEmpty(detailJson) || isEmpty(conditionJson);
    }

    private void apply(JsonNode listJson, JsonNode detailJson, JsonNode conditionJson, Instant fetchedAt) {
        this.listJson = listJson == null ? normalize(this.listJson) : listJson;
        this.detailJson = detailJson == null ? normalize(this.detailJson) : detailJson;
        this.conditionJson = conditionJson == null ? normalize(this.conditionJson) : conditionJson;
        this.fetchedAt = fetchedAt;
        // null 인자로 보존된 기존 값까지 포함해 실제 저장 내용과 같은 해시를 만든다.
        this.payloadHash = hash(this.listJson, this.detailJson, this.conditionJson);
    }

    /**
     * 세 원문을 이어붙인 SHA-256.
     *
     * <p>이관 마이그레이션은 이 값을 채우지 않는다. Postgres가 jsonb를 text로 바꿀 때 넣는 공백 때문에
     * 같은 내용이어도 문자열이 달라져 비교할 수 없기 때문이다. 따라서 이관 직후 첫 동기화는 모든 사업을
     * "바뀜"으로 보며, 그 뒤부터 실제 변경만 가려낸다.
     */
    private static String hash(JsonNode listJson, JsonNode detailJson, JsonNode conditionJson) {
        String joined = text(listJson) + text(detailJson) + text(conditionJson);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            // 표준 JDK에는 항상 포함되어 있어 실제로는 발생하지 않는다.
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static String text(JsonNode node) {
        return node == null ? "{}" : node.toString();
    }

    private static JsonNode normalize(JsonNode node) {
        return node == null ? JsonNodeFactory.instance.objectNode() : node;
    }

    private static boolean isEmpty(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() || (node.isObject() && node.isEmpty());
    }
}
