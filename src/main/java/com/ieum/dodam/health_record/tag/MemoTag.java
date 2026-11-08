package com.ieum.dodam.health_record.tag;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="memo_tags") @Getter @NoArgsConstructor
public class MemoTag {
 @Id private UUID id;
 @Column(nullable=false) private UUID patientId;
 @Column(nullable=false,length=20) private String name;
 private Instant deletedAt;
 @Version private long version;
 public static MemoTag create(UUID patientId,String name){var t=new MemoTag();t.id=UUID.randomUUID();t.patientId=patientId;t.name=name;return t;}
 public void rename(String name){this.name=name;}
 public void delete(){deletedAt=Instant.now();}
}
