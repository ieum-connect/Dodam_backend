package com.ieum.dodam.notification.push;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "push_devices") @Getter @NoArgsConstructor
public class PushDevice {
    @Id private String id;
    @Column(nullable=false) private UUID userId;
    @Column(nullable=false, length=4096) private String token;
    @Column(nullable=false) private boolean active;
    @Column(name="show_details", nullable=false) private boolean showDetails;
    @Column(nullable=false) private Instant updatedAt;
    public PushDevice(String id, UUID userId, String token) { this.id=id; bind(userId,token); }
    public void bind(UUID userId, String token) {
        if (!active || !userId.equals(this.userId)) showDetails = false;
        this.userId = userId;
        this.token = token;
        active = true;
        updatedAt = Instant.now();
    }
    public void changeDetailsVisibility(boolean showDetails) { this.showDetails = showDetails; }
    public void disable() { active=false; }
}
