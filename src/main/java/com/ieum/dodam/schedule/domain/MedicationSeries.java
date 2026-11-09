package com.ieum.dodam.schedule.domain;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="medication_series") @Getter @NoArgsConstructor
public class MedicationSeries {
    @Id private UUID id;
    @Column(nullable=false) private UUID patientId;
    @Column(nullable=false,length=200) private String title;
    @Column(length=500) private String notes;
    @Column(nullable=false) private LocalDate startDate;
    @Column(nullable=false) private LocalTime timeOfDay;
    @Column(nullable=false,length=80) private String timeZone;
    private LocalDate endDate;
    @Column(nullable=false) private LocalDate nextDate;
    @Column(nullable=false) private boolean active;
    @Version private long version;
    public static MedicationSeries create(UUID id, UUID patientId, String title, String notes, Instant startsAt, ZoneId zone, LocalDate endDate) {
        var s=new MedicationSeries(); s.id=id; s.patientId=patientId; s.title=title.strip(); s.notes=notes;
        var start=startsAt.atZone(zone); s.startDate=start.toLocalDate(); s.timeOfDay=start.toLocalTime().withNano(0); s.timeZone=zone.getId();
        s.endDate=endDate; s.nextDate=s.startDate; s.active=true; return s;
    }
    public void advance(LocalDate next) { nextDate=next; }
    public void stop() { active=false; }
}
