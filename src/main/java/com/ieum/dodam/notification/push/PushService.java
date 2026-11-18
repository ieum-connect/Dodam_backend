package com.ieum.dodam.notification.push;

import com.ieum.dodam.notification.domain.entity.SystemNotification;
import com.ieum.dodam.schedule.domain.Schedule;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.user.domain.repository.UserRepository;
import com.ieum.dodam.user.domain.UserStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.UUID;

@Service
public class PushService {
    private final PushDeviceRepository devices;
    private final PushDeliveryRepository deliveries;
    private final ScheduleRepository schedules;
    private final PatientRepository patients;
    private final CareRelationRepository relations;
    private final UserRepository users;
    private final ObjectProvider<PushSender> sender;
    public PushService(PushDeviceRepository devices,PushDeliveryRepository deliveries,ScheduleRepository schedules,
            PatientRepository patients,CareRelationRepository relations,UserRepository users,ObjectProvider<PushSender> sender) {
        this.devices=devices; this.deliveries=deliveries; this.schedules=schedules;
        this.patients=patients; this.relations=relations; this.users=users; this.sender=sender;
    }
    @Transactional
    public void enqueue(SystemNotification notification,Schedule schedule) {
        if(sender.getIfAvailable()==null || schedule.getStartsAt().plusSeconds(900).isBefore(Instant.now())) return;
        for(var device:devices.findByUserIdAndActiveTrue(notification.getRecipientUserId())) {
            deliveries.save(new PushDelivery(notification.getId(),notification.getRecipientUserId(),device.getId(),schedule.getId(),schedule.getStartsAt()));
        }
    }
    @Transactional
    public boolean deliverOne(Instant now) {
        var transport=sender.getIfAvailable();
        if(transport==null) return false;
        var rows=deliveries.lockDue(now,PageRequest.of(0,1));
        if(rows.isEmpty()) return false;
        var delivery=rows.get(0);
        var device=devices.findById(delivery.getDeviceId()).orElse(null);
        var schedule=schedules.findById(delivery.getScheduleId()).orElse(null);
        if(!eligible(delivery,device,schedule,now)) { delivery.finish("SKIPPED"); return true; }
        PushSender.Result result;
        var reminder = new PushSender.Reminder(delivery.getNotificationId(), schedule.getScheduleType(),
            device.isShowDetails() ? schedule.getTitle() : null);
        try { result=transport.send(device.getToken(),reminder); }
        catch(RuntimeException e) { result=PushSender.Result.RETRY; }
        switch(result) {
            case SENT -> delivery.finish("SENT");
            case INVALID_TOKEN -> { device.disable(); delivery.finish("INVALID_TOKEN"); }
            case RETRY -> delivery.retry(now);
        }
        return true;
    }
    boolean eligible(PushDelivery d,PushDevice device,Schedule schedule,Instant now) {
        if(device==null || !device.isActive() || !device.getUserId().equals(d.getUserId())
            || !now.isBefore(d.getExpiresAt()) || schedule==null
            || !"SCHEDULED".equals(schedule.getStatus()) || !schedule.getStartsAt().equals(d.getDueAt())) return false;
        if(users.findById(d.getUserId()).filter(u->u.getStatus()==UserStatus.ACTIVE).isEmpty()) return false;
        return patients.findById(schedule.getPatientId()).map(p -> p.getUserId().equals(d.getUserId())
            || relations.existsByPatientIdAndCaregiverIdAndStatus(p.getId(),d.getUserId(),CareRelationStatus.ACTIVE)).orElse(false);
    }
}
