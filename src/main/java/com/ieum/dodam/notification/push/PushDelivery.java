package com.ieum.dodam.notification.push;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="push_deliveries", uniqueConstraints=@UniqueConstraint(columnNames={"notification_id","device_id"}))
@Getter @NoArgsConstructor
public class PushDelivery {
    @Id private UUID id;
    @Column(nullable=false) private UUID notificationId;
    @Column(nullable=false) private UUID userId;
    @Column(nullable=false) private String deviceId;
    @Column(nullable=false) private UUID scheduleId;
    @Column(nullable=false) private Instant dueAt;
    @Column(nullable=false) private Instant nextAttemptAt;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private int attempts;
    @Column(nullable=false) private String status;
    public PushDelivery(UUID notificationId, UUID userId, String deviceId, UUID scheduleId, Instant dueAt) {
        id=UUID.randomUUID(); this.notificationId=notificationId; this.userId=userId;
        this.deviceId=deviceId; this.scheduleId=scheduleId; this.dueAt=dueAt;
        nextAttemptAt=Instant.now(); expiresAt=dueAt.plusSeconds(900); status="PENDING";
    }
    public void finish(String status) { this.status=status; }
    public void retry(Instant now) { attempts++; if(attempts>=5) status="FAILED"; else nextAttemptAt=now.plusSeconds(30L << (attempts-1)); }
}
