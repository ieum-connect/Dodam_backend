package com.ieum.dodam.notification.push;

import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.*;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.schedule.domain.*;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import com.ieum.dodam.schedule.command.application.ScheduleReminderService;
import com.ieum.dodam.global.auth.application.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="dodam.schedule.reminders-enabled=false") @AutoConfigureMockMvc @Transactional
class PushIntegrationTest {
    @Autowired UserAccountService accounts;
    @Autowired PatientRepository patients;
    @Autowired ScheduleRepository schedules;
    @Autowired ScheduleReminderService reminders;
    @Autowired PushService push;
    @Autowired PushDeviceRepository devices;
    @Autowired PushDeliveryRepository deliveries;
    @Autowired PushApiController controller;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @MockBean PushSender sender;
    User owner;
    UUID patientId;
    @BeforeEach void setup() {
        owner=accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(),"push-test").getKakaoId(),UserRole.PATIENT);
        patientId=patients.findByUserId(owner.getId()).orElseThrow().getId();
        ReflectionTestUtils.setField(controller,"enabled",true);
        when(sender.send(anyString(),any())).thenReturn(PushSender.Result.SENT);
    }
    @AfterEach void resetFlag() { ReflectionTestUtils.setField(controller,"enabled",false); }
    @Test void medicationAndAppointmentQueueExactlyOnceAndSend() {
        devices.save(new PushDevice("device",owner.getId(),"test-token"));
        var now=Instant.now();
        schedules.save(Schedule.create(patientId,null,ScheduleType.MEDICATION,"medicine",now.minusSeconds(2)));
        schedules.save(Schedule.create(patientId,null,ScheduleType.REVISIT,"appointment",now.minusSeconds(1)));
        reminders.deliverDue(now); reminders.deliverDue(now);
        assertEquals(2,deliveries.count());
        assertTrue(push.deliverOne(now.plusSeconds(1))); assertTrue(push.deliverOne(now.plusSeconds(1)));
        assertFalse(push.deliverOne(now.plusSeconds(1)));
        verify(sender,times(2)).send(eq("test-token"),any());
        assertTrue(deliveries.findAll().stream().allMatch(d->d.getStatus().equals("SENT")));
    }
    @Test void revokedTokenIsDisabledAndFailedSendRetries() {
        devices.save(new PushDevice("device",owner.getId(),"test-token"));
        var now=Instant.now(); schedules.save(Schedule.create(patientId,null,ScheduleType.MEDICATION,"medicine",now.minusSeconds(1)));
        reminders.deliverDue(now);
        when(sender.send(anyString(),any())).thenReturn(PushSender.Result.RETRY,PushSender.Result.INVALID_TOKEN);
        push.deliverOne(now.plusSeconds(1)); assertEquals(1,deliveries.findAll().get(0).getAttempts());
        assertFalse(push.deliverOne(now.plusSeconds(2)));
        push.deliverOne(now.plusSeconds(32)); assertFalse(devices.findById("device").orElseThrow().isActive());
    }
    @Test void disabledPushReturnsCommonError() throws Exception {
        ReflectionTestUtils.setField(controller,"enabled",false);
        mvc.perform(post("/api/push/devices")
                .header("Authorization","Bearer "+jwt.create(owner))
                .contentType("application/json").content("{\"token\":\"example\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("PUSH_DISABLED"))
            .andExpect(jsonPath("$.message").isNotEmpty());
        assertTrue(devices.findById(PushApiController.hash("example")).isEmpty());
        verifyNoInteractions(sender);
    }
    @Test void registrationRequiresAuthenticationAndUnregisterIsOwned() throws Exception {
        mvc.perform(post("/api/push/devices").contentType("application/json").content("{\"token\":\"example\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/push/devices").header("Authorization","Bearer "+jwt.create(owner)).contentType("application/json").content("{\"token\":\"example\"}")).andExpect(status().isNoContent());
        assertEquals(owner.getId(),devices.findById(PushApiController.hash("example")).orElseThrow().getUserId());
        var other=accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(),"other").getKakaoId(),UserRole.CAREGIVER);
        mvc.perform(delete("/api/push/devices").header("Authorization","Bearer "+jwt.create(other)).contentType("application/json").content("{\"token\":\"example\"}")).andExpect(status().isNoContent());
        assertTrue(devices.findById(PushApiController.hash("example")).orElseThrow().isActive());
        mvc.perform(delete("/api/push/devices").header("Authorization","Bearer "+jwt.create(owner)).contentType("application/json").content("{\"token\":\"example\"}")).andExpect(status().isNoContent());
        assertFalse(devices.findById(PushApiController.hash("example")).orElseThrow().isActive());
    }
}
