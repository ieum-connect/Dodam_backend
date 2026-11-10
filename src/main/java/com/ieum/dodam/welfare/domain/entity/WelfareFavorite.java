package com.ieum.dodam.welfare.domain.entity;
import jakarta.persistence.*; import lombok.AccessLevel; import lombok.Getter; import lombok.NoArgsConstructor; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="welfare_favorites") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class WelfareFavorite { @Id private UUID id; @Column(name="patient_id",nullable=false) private UUID patientId; @Column(name="program_id",nullable=false) private UUID programId; @Column(name="created_at",nullable=false) private Instant createdAt; public static WelfareFavorite create(UUID patientId,UUID programId){ WelfareFavorite f=new WelfareFavorite(); f.id=UUID.randomUUID(); f.patientId=patientId; f.programId=programId; f.createdAt=Instant.now(); return f; } }
