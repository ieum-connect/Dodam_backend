package com.ieum.dodam.schedule.command.application;

import com.ieum.dodam.action_item.domain.ActionItemApprovalStatus;
import com.ieum.dodam.action_item.domain.entity.ActionItem;
import com.ieum.dodam.action_item.domain.repository.ActionItemRepository;
import com.ieum.dodam.global.exception.*;
import com.ieum.dodam.medical_document.domain.repository.MedicalDocumentRepository;
import com.ieum.dodam.schedule.domain.Schedule;
import com.ieum.dodam.schedule.domain.ScheduleBatch;
import com.ieum.dodam.schedule.domain.ScheduleType;
import com.ieum.dodam.schedule.domain.repository.ScheduleBatchRepository;
import com.ieum.dodam.schedule.domain.repository.ScheduleRepository;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.visit.domain.VisitAccessPolicy;
import com.ieum.dodam.visit.domain.repository.VisitRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CalendarService {
    private final ActionItemRepository actions;
    private final ScheduleRepository schedules;
    private final VisitRepository visits;
    private final MedicalDocumentRepository documents;
    private final VisitAccessPolicy access;
    private final ScheduleBatchRepository batches;
    private final MedicationSeriesService recurring;

    public CalendarService(
            ActionItemRepository actions,
            ScheduleRepository schedules,
            VisitRepository visits,
            MedicalDocumentRepository documents,
            VisitAccessPolicy access,
            ScheduleBatchRepository batches, MedicationSeriesService recurring) {
        this.actions = actions;
        this.schedules = schedules;
        this.visits = visits;
        this.documents = documents;
        this.access = access;
        this.batches = batches;
        this.recurring = recurring;
    }

    public List<ActionItem> candidates(User actor, UUID patientId) {
        return actions.findForPatient(access.requirePatient(actor, patientId));
    }

    public List<Schedule> list(User actor, UUID patientId, Instant from, Instant to) {
        access.requirePatient(actor, patientId);
        if (!from.isBefore(to) || to.isAfter(from.plusSeconds(370L * 86400))) throw invalid();
        recurring.ensureRange(patientId,from,to);
        return schedules
                .findByPatientIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        patientId, from, to);
    }

    public Schedule create(
            User actor, UUID patientId, String title, ScheduleType type, Instant startsAt) {
        access.requirePatient(actor, patientId);
        if (startsAt == null
                || title == null
                || title.isBlank()
                || title.length() > 200
                || type == null) throw invalid();
        return schedules.save(Schedule.create(patientId, null, type, title.strip(), startsAt));
    }

    public List<Schedule> createBatch(
            User actor,
            UUID patientId,
            UUID requestId,
            String title,
            ScheduleType type,
            Instant startsAt,
            String notes,
            int repeatDays,
            String timeZone) {
        access.requirePatient(actor, patientId);
        validateWrite(title, type, startsAt, notes);
        if (requestId == null
                || repeatDays < 1
                || repeatDays > 31
                || (repeatDays > 1 && type != ScheduleType.MEDICATION)) throw invalid();
        java.time.ZoneId zone;
        try {
            zone = java.time.ZoneId.of(timeZone);
        } catch (java.time.DateTimeException | NullPointerException e) {
            throw invalid();
        }
        schedules.lockPatient(patientId).orElseThrow(CalendarService::invalid);
        String fingerprint =
                org.springframework.util.DigestUtils.md5DigestAsHex(
                        (title.strip()
                                        + "\n"
                                        + type
                                        + "\n"
                                        + startsAt
                                        + "\n"
                                        + notes
                                        + "\n"
                                        + repeatDays
                                        + "\n"
                                        + zone)
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var existing = batches.findById(requestId);
        if (existing.isPresent()) {
            if (!existing.get().getPatientId().equals(patientId)
                    || !existing.get().getFingerprint().equals(fingerprint)) throw conflict();
            return schedules.findByBatchIdOrderByStartsAtAsc(requestId);
        }
        batches.saveAndFlush(new ScheduleBatch(requestId, patientId, fingerprint));
        var result = new java.util.ArrayList<Schedule>();
        for (int day = 0; day < repeatDays; day++) {
            Instant date = startsAt.atZone(zone).plusDays(day).toInstant();
            var row = Schedule.create(patientId, null, type, title.strip(), date);
            row.update(title.strip(), type, date, notes);
            row.assignBatch(requestId);
            result.add(schedules.save(row));
        }
        return result;
    }

    public Schedule update(
            User actor,
            UUID id,
            long version,
            String title,
            ScheduleType type,
            Instant startsAt,
            String notes) {
        var row = requireSchedule(actor, id);
        if (row.getVersion() != version || !row.getStatus().equals("SCHEDULED")) throw conflict();
        validateWrite(title, type, startsAt, notes);
        row.update(title.strip(), type, startsAt, notes);
        return schedules.saveAndFlush(row);
    }

    public Schedule finish(User actor, UUID id, boolean complete) {
        var row = requireSchedule(actor, id);
        String target = complete ? "COMPLETED" : "CANCELLED";
        if (row.getStatus().equals(target)) return row;
        if (!row.getStatus().equals("SCHEDULED")) throw conflict();
        row.finish(complete);
        return schedules.saveAndFlush(row);
    }

    private Schedule requireSchedule(User actor, UUID id) {
        var row = schedules.lockById(id).orElseThrow(CalendarService::invalid);
        access.requirePatient(actor, row.getPatientId());
        return row;
    }

    private static void validateWrite(
            String title, ScheduleType type, Instant startsAt, String notes) {
        if (startsAt == null
                || type == null
                || title == null
                || title.isBlank()
                || title.length() > 200
                || (notes != null && notes.length() > 500)) throw invalid();
    }

    public Schedule decide(User actor, UUID id, boolean approve, Instant startsAt) {
        var item = actions.lockById(id).orElseThrow(CalendarService::invalid);
        var visit = visits.findById(item.getVisitId()).orElseThrow(CalendarService::invalid);
        access.requirePatient(actor, visit.getPatientId());
        var document =
                documents.findById(item.getDocumentId()).orElseThrow(CalendarService::invalid);
        if (!document.getVisitId().equals(visit.getId())
                || !document.getPatientId().equals(visit.getPatientId())) throw conflict();
        if (approve && item.getApprovalStatus() == ActionItemApprovalStatus.APPROVED)
            return schedules.findByActionItemId(id).orElseThrow(CalendarService::conflict);
        if (!approve && item.getApprovalStatus() == ActionItemApprovalStatus.REJECTED) return null;
        if (item.getApprovalStatus() != ActionItemApprovalStatus.PENDING
                || document.getDeletedAt() != null
                || visit.getDeletedAt() != null) throw conflict();
        if (!approve) {
            item.reject();
            return null;
        }
        Instant date = startsAt != null ? startsAt : item.getDueAt();
        if (date == null) throw invalid();
        ScheduleType type =
                switch (item.getType()) {
                    case MEDICATION -> ScheduleType.MEDICATION;
                    case REVISIT -> ScheduleType.REVISIT;
                    case TEST -> ScheduleType.TEST;
                    case CAUTION, OTHER -> ScheduleType.OTHER;
                };
        item.approve(actor.getId(), Instant.now());
        return schedules.save(
                Schedule.create(visit.getPatientId(), id, type, item.getTitle(), date));
    }

    private static BusinessException invalid() {
        return new BusinessException(GlobalErrorCode.INVALID_REQUEST);
    }

    private static BusinessException conflict() {
        return new BusinessException(GlobalErrorCode.CONFLICT);
    }
}
