package com.ieum.dodam.schedule;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieum.dodam.global.auth.application.JwtService;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.domain.*;
import com.ieum.dodam.schedule.command.application.MedicationSeriesService;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@SpringBootTest(properties="dodam.schedule.reminders-enabled=false") @AutoConfigureMockMvc @Transactional
class MeetingFollowupIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserAccountService accounts; @Autowired JwtService jwt; @Autowired PatientRepository patients;
 @Autowired MedicationSeriesService recurring; @Autowired ScheduleRepository schedules;
 @Autowired jakarta.persistence.EntityManager em;
 @Autowired com.ieum.dodam.care_relation.command.application.CareRelationCommandService cares;
 User owner; UUID patient; String token;
 final ZoneId zone=ZoneId.of("Asia/Seoul");
 @BeforeEach void setup(){owner=accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(),"회의 검증").getKakaoId(),UserRole.PATIENT);patient=patients.findByUserId(owner.getId()).orElseThrow().getId();token=jwt.create(owner);}
 LocalDate today(){return LocalDate.now(zone);}
 Instant start(){return today().atTime(9,0).atZone(zone).toInstant();}
 String tag(String name)throws Exception{return json.readTree(mvc.perform(post("/api/memo-tags").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("patientId",patient,"name",name)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asText();}
 @Test void finiteSeriesSupportsBeyond31DaysAndQueryDoesNotRegenerateCancelledOccurrence() throws Exception {
  UUID id=UUID.randomUUID();var rule=recurring.create(owner,patient,id,"복약",null,start(),zone.getId(),today().plusDays(59));
  assertThat(schedules.occurrenceDates(id,today(),today().plusDays(100))).hasSize(32);
  recurring.ensureRange(patient,start(),today().plusDays(90).atStartOfDay(zone).toInstant());
  assertThat(schedules.occurrenceDates(id,today(),today().plusDays(100))).hasSize(60);
  var row=schedules.findByPatientIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(patient,start(),start().plusSeconds(86400)).get(0);row.finish(false);schedules.flush();
  recurring.ensureRange(patient,start(),start().plusSeconds(86400));
  assertThat(schedules.occurrenceDates(id,today(),today().plusDays(100))).hasSize(60);
  assertThat(recurring.create(owner,patient,id,"복약",null,start(),zone.getId(),today().plusDays(59)).getId()).isEqualTo(rule.getId());
  assertThatThrownBy(()->recurring.create(owner,patient,id,"다른 약",null,start(),zone.getId(),null)).isInstanceOf(RuntimeException.class);
 }
 @Test void indefiniteSeriesIsBoundedAndStoppingPreservesCompletedHistory() {
  UUID id=UUID.randomUUID();recurring.create(owner,patient,id,"매일 복약",null,start(),zone.getId(),null);
  assertThat(schedules.occurrenceDates(id,today(),today().plusYears(5))).hasSize(32);
  recurring.replenish(id,start().plusSeconds(60L*86400));
  assertThat(schedules.occurrenceDates(id,today(),today().plusYears(5))).hasSize(92);
  var row=schedules.findByPatientIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(patient,start(),start().plusSeconds(86400)).get(0);row.finish(true);schedules.flush();
  recurring.stop(owner,id); recurring.ensureRange(patient,start(),today().plusYears(1).atStartOfDay(zone).toInstant());
  assertThat(row.getStatus()).isEqualTo("COMPLETED");
  assertThat(schedules.occurrenceDates(id,today(),today().plusYears(5))).hasSize(92);
  assertThat(schedules.findByPatientIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(patient,Instant.now().plusSeconds(86400),start().plusSeconds(100L*86400))).allMatch(s->s.getStatus().equals("CANCELLED"));
 }
 @Test void invalidEndDateAndForeignRuleAccessAreRejected()throws Exception {
  var body=new HashMap<String,Object>();body.put("patientId",patient);body.put("requestId",UUID.randomUUID());body.put("title","복약");body.put("startsAt",start().toString());body.put("timeZone",zone.getId());body.put("endDate",today().minusDays(1).toString());
  mvc.perform(post("/api/medication-series").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
  var rule=recurring.create(owner,patient,UUID.randomUUID(),"복약",null,start(),zone.getId(),null);
  User other=accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(),"타인").getKakaoId(),UserRole.PATIENT);
  mvc.perform(post("/api/medication-series/"+rule.getId()+"/stop").header("Authorization","Bearer "+jwt.create(other))).andExpect(status().isForbidden());
 }
 @Test void tagsPersistRenameAndDeleteWithoutDeletingMemo()throws Exception {
  String tid=tag("수유");
  var payload=new HashMap<String,Object>();payload.put("patientId",patient);payload.put("body","자유 메모");payload.put("tagIds",List.of(tid));
  String rid=json.readTree(mvc.perform(post("/api/health-records").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(payload))).andExpect(status().isCreated()).andExpect(jsonPath("$.tagIds[0]").value(tid)).andReturn().getResponse().getContentAsString()).get("recordId").asText();
  mvc.perform(put("/api/memo-tags/"+tid).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("patientId",patient,"name","식사","version",0)))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("식사"));
  mvc.perform(delete("/api/memo-tags/"+tid).param("version","1").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());em.flush();em.clear();
  mvc.perform(get("/api/health-records/"+rid).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.body").value("자유 메모")).andExpect(jsonPath("$.tagIds.length()").value(0));
 }
 @Test void duplicateTagAndForeignTagAttachmentRejected()throws Exception {
  String tid=tag("수면");
  mvc.perform(post("/api/memo-tags").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("patientId",patient,"name"," 수면 ")))).andExpect(status().isConflict());
  User other=accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(),"다른 아이").getKakaoId(),UserRole.PATIENT);UUID otherPatient=patients.findByUserId(other.getId()).orElseThrow().getId();
  mvc.perform(post("/api/health-records").header("Authorization","Bearer "+jwt.create(other)).contentType("application/json").content(json.writeValueAsString(Map.of("patientId",otherPatient,"body","타인 태그 거절","tagIds",List.of(tid))))).andExpect(status().isBadRequest());
 }
 @Test void activeFamilySharesTagsAndRevocationRemovesAccess()throws Exception {
  tag("복약");User family=accounts.chooseRole(accounts.loginOrRegister(UUID.randomUUID().toString(),"가족").getKakaoId(),UserRole.CAREGIVER);
  var relation=cares.request(family,accounts.patientInviteCode(owner));cares.accept(owner,relation.getId());
  mvc.perform(get("/api/memo-tags").param("patientId",patient.toString()).header("Authorization","Bearer "+jwt.create(family))).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("복약"));
  cares.revoke(owner,relation.getId());
  mvc.perform(get("/api/memo-tags").param("patientId",patient.toString()).header("Authorization","Bearer "+jwt.create(family))).andExpect(status().isForbidden());
 }
}
