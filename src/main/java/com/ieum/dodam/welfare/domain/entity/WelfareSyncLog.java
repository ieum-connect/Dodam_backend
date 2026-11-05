package com.ieum.dodam.welfare.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 외부 API 호출 단위 동기화 결과. */
@Entity @Table(name="welfare_sync_logs")
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class WelfareSyncLog {
    public enum Endpoint { SERVICE_LIST, SERVICE_DETAIL, SUPPORT_CONDITIONS }
    public enum SyncType { FULL, INCREMENTAL, DETAIL_REFRESH }
    public enum Status { STARTED, SUCCEEDED, FAILED, PARTIAL }

    @Id private UUID id;
    @Column(name="program_id") private UUID programId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Endpoint endpoint;
    @Enumerated(EnumType.STRING) @Column(name="sync_type",nullable=false) private SyncType syncType;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Status status;
    @Column(name="requested_page") private Long requestedPage;
    @Column(name="received_count") private Long receivedCount;
    @Column(name="error_code",length=100) private String errorCode;
    @Column(name="error_message",columnDefinition="TEXT") private String errorMessage;
    @Column(name="started_at",nullable=false) private Instant startedAt;
    @Column(name="finished_at") private Instant finishedAt;
}
