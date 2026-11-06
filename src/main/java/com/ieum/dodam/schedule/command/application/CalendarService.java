package com.ieum.dodam.schedule.command.application;

import com.ieum.dodam.action_item.domain.ActionItemApprovalStatus;
import com.ieum.dodam.action_item.domain.entity.ActionItem;
import com.ieum.dodam.action_item.domain.repository.ActionItemRepository;
import com.ieum.dodam.global.exception.*;
import com.ieum.dodam.medical_document.domain.repository.MedicalDocumentRepository;
import com.ieum.dodam.schedule.domain.Schedule;
import com.ieum.dodam.schedule.domain.ScheduleType;
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

    public CalendarService(
            ActionItemRepository actions,
            ScheduleRepository schedules,
            VisitRepository visits,
            MedicalDocumentRepository documents,
            VisitAccessPolicy access) {
        this.actions = actions;
        this.schedules = schedules;
        this.visits = visits;
        this.documents = documents;
        this.access = access;
    }

    public List<ActionItem> candidates(User actor, UUID patientId) {
        return actions.findForPatient(access.requirePatient(actor, patientId));
    }

    public List<Schedule> list(User actor, UUID patientId, Instant from, Instant to) {
        access.requirePatient(actor, patientId);
        if (!from.isBefore(to) || to.isAfter(from.plusSeconds(370L * 86400))) throw invalid();
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
