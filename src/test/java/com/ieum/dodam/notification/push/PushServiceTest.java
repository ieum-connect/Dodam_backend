package com.ieum.dodam.notification.push;

import com.ieum.dodam.schedule.domain.*;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.user.domain.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PushServiceTest {
    PushDeviceRepository devices=mock(PushDeviceRepository.class);
    PushDeliveryRepository deliveries=mock(PushDeliveryRepository.class);
    ScheduleRepository schedules=mock(ScheduleRepository.class);
    PatientRepository patients=mock(PatientRepository.class);
    CareRelationRepository relations=mock(CareRelationRepository.class);
    UserRepository users=mock(UserRepository.class);
    @SuppressWarnings("unchecked") ObjectProvider<PushSender> provider=mock(ObjectProvider.class);
    PushSender sender=mock(PushSender.class);
    PushService service=new PushService(devices,deliveries,schedules,patients,relations,users,provider);
    @Test void disabledConfigurationDoesNotSendOrReadQueue() {
        assertFalse(service.deliverOne(Instant.now())); verifyNoInteractions(deliveries,sender);
    }
    @Test void expiredCancelledMovedOrWrongOwnerNeverSend() {
        var now=Instant.now(); var user=UUID.randomUUID();
        var schedule=Schedule.create(UUID.randomUUID(),null,ScheduleType.MEDICATION,"private title",now);
        var row=new PushDelivery(UUID.randomUUID(),user,"device",schedule.getId(),now);
        var device=new PushDevice("device",user,"private-token");
        assertFalse(service.eligible(row,device,schedule,now.plusSeconds(901)));
        device.bind(UUID.randomUUID(),"private-token"); assertFalse(service.eligible(row,device,schedule,now));
        device.bind(user,"private-token"); schedule.finish(true); assertFalse(service.eligible(row,device,schedule,now));
        schedule=Schedule.create(UUID.randomUUID(),null,ScheduleType.MEDICATION,"private title",now.plusSeconds(60));
        assertFalse(service.eligible(row,device,schedule,now)); verifyNoInteractions(sender);
    }
    @Test void retryIsBoundedAndBacksOff() {
        var now=Instant.now(); var row=new PushDelivery(UUID.randomUUID(),UUID.randomUUID(),"device",UUID.randomUUID(),now);
        row.retry(now); assertEquals(now.plusSeconds(30),row.getNextAttemptAt());
        row.retry(now); assertEquals(now.plusSeconds(60),row.getNextAttemptAt());
        row.retry(now); row.retry(now); row.retry(now); assertEquals("FAILED",row.getStatus());
    }
    @Test void missingDeviceIsSkippedWithoutNetwork() {
        var now=Instant.now(); var row=new PushDelivery(UUID.randomUUID(),UUID.randomUUID(),"missing",UUID.randomUUID(),now);
        when(provider.getIfAvailable()).thenReturn(sender); when(deliveries.lockDue(eq(now),any())).thenReturn(List.of(row));
        assertTrue(service.deliverOne(now)); assertEquals("SKIPPED",row.getStatus()); verifyNoInteractions(sender);
    }
}
