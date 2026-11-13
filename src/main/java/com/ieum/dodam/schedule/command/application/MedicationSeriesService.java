package com.ieum.dodam.schedule.command.application;
import com.ieum.dodam.schedule.domain.*;
import com.ieum.dodam.schedule.domain.repository.*;
import com.ieum.dodam.visit.domain.VisitAccessPolicy;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.global.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @Transactional
public class MedicationSeriesService {
    private final MedicationSeriesRepository series;
    private final ScheduleRepository schedules;
    private final VisitAccessPolicy access;
    public MedicationSeriesService(MedicationSeriesRepository series, ScheduleRepository schedules, VisitAccessPolicy access) {
        this.series=series; this.schedules=schedules; this.access=access;
    }
    public MedicationSeries create(User actor, UUID patientId, UUID requestId, String title, String notes, Instant startsAt, String timeZone, LocalDate endDate) {
        return create(actor, patientId, requestId, title, notes, startsAt, timeZone, endDate, 127);
    }
    public MedicationSeries create(User actor, UUID patientId, UUID requestId, String title, String notes, Instant startsAt, String timeZone, LocalDate endDate, int weekdays) {
        access.requirePatient(actor,patientId);
        requireWeekdays(weekdays);
        ZoneId zone;
        try { zone=ZoneId.of(timeZone); } catch(Exception e) { throw invalid(); }
        if(requestId==null || startsAt==null || title==null || title.isBlank() || title.length()>200 || (notes!=null && notes.length()>500)) throw invalid();
        LocalDate start=startsAt.atZone(zone).toLocalDate();
        // A recurring rule starts today or later. Historic one-off records use the existing schedule API.
        if(start.isBefore(LocalDate.now(zone)) || start.getYear()>9998 || (endDate!=null && (endDate.isBefore(start) || endDate.getYear()>9998))) throw invalid();
        schedules.lockPatient(patientId).orElseThrow(MedicationSeriesService::invalid);
        var desired=MedicationSeries.create(requestId,patientId,title,notes,startsAt,zone,endDate);
        desired.changeRule(weekdays, endDate, desired.getTimeOfDay(), start);
        var existing=series.findById(requestId);
        if(existing.isPresent()) {
            var s=existing.get();
            if(!s.getPatientId().equals(patientId) || !s.getTitle().equals(desired.getTitle()) || !Objects.equals(s.getNotes(),notes)
                || !s.getStartDate().equals(start) || !s.getTimeOfDay().equals(desired.getTimeOfDay()) || !s.getTimeZone().equals(zone.getId()) || !Objects.equals(s.getEndDate(),endDate)
                || s.getWeekdayMask() != weekdays)
                throw new BusinessException(GlobalErrorCode.CONFLICT);
            return s;
        }
        // Assigned UUIDs may be merged; advance the managed instance returned by save.
        desired=series.saveAndFlush(desired);
        materialize(desired,start,start.plusDays(31));
        desired.advance(start.plusDays(32));
        return desired;
    }
    @Transactional(readOnly=true)
    public List<MedicationSeries> list(User actor,UUID patientId) { access.requirePatient(actor,patientId); return series.findByPatientIdOrderByStartDateDesc(patientId); }
    public void ensureRange(UUID patientId,Instant from,Instant to) {
        if (from == null || to == null || !from.isBefore(to)
                || to.isAfter(from.plusSeconds(92L * 86400))) throw invalid();
        for(var item:series.findByPatientIdOrderByStartDateDesc(patientId)) {
            if (!item.isActive()) continue;
            var s=series.lockById(item.getId()).orElseThrow();
            var zone=ZoneId.of(s.getTimeZone());
            materialize(s,from.atZone(zone).toLocalDate(),to.minusNanos(1).atZone(zone).toLocalDate());
        }
    }
    // Bounded batches catch up after downtime. Unique occurrence keys and a rule lock prevent duplicate reminders.
    public void replenish(UUID id,Instant now) {
        var s=series.lockById(id).orElseThrow();
        LocalDate through=now.atZone(ZoneId.of(s.getTimeZone())).toLocalDate().plusDays(31);
        if(s.getNextDate().isAfter(through) || !s.isActive() || (s.getEndDate()!=null && s.getNextDate().isAfter(s.getEndDate()))) return;
        LocalDate end=s.getNextDate().plusDays(91);
        if(end.isAfter(through)) end=through;
        materialize(s,s.getNextDate(),end); s.advance(end.plusDays(1));
    }
    public MedicationSeries stop(User actor,UUID id) {
        var s=series.lockById(id).orElseThrow(MedicationSeriesService::invalid);
        access.requirePatient(actor,s.getPatientId());
        if(s.isActive()) {
            s.stop();
            for(var row:schedules.lockFutureSeries(id,Instant.now())) if(row.getStatus().equals("SCHEDULED")) row.finish(false);
        }
        return s;
    }
    public MedicationSeries update(User actor, UUID id, long version, int weekdays, LocalTime time, LocalDate endDate) {
        requireWeekdays(weekdays);
        var rule = series.lockById(id).orElseThrow(MedicationSeriesService::invalid);
        access.requirePatient(actor, rule.getPatientId());
        if (!rule.isActive() || rule.getVersion() != version) throw new BusinessException(GlobalErrorCode.CONFLICT);
        var now = Instant.now();
        var zone = ZoneId.of(rule.getTimeZone());
        var today = now.atZone(zone).toLocalDate();
        if (time == null) throw invalid();
        var firstPossibleDate = today.atTime(time).atZone(zone).toInstant().isBefore(now) ? today.plusDays(1) : today;
        var effectiveFrom = firstPossibleDate.isBefore(rule.getStartDate()) ? rule.getStartDate() : firstPossibleDate;
        if (endDate != null && (endDate.isBefore(effectiveFrom) || endDate.getYear() > 9998)) throw invalid();
        var futureRows = schedules.lockFutureSeries(id, now);
        rule.changeRule(weekdays, endDate, time, effectiveFrom);
        for (var row : futureRows) {
            var day = row.getOccurrenceDate();
            if (!"SCHEDULED".equals(row.getStatus()) || row.getRemindedAt() != null) continue;
            var startsAt = day.atTime(time).atZone(zone).toInstant();
            if (!rule.includes(day) || startsAt.isBefore(now)) row.removeFromSeries();
            else row.update(rule.getTitle(), ScheduleType.MEDICATION, startsAt, rule.getNotes());
        }
        schedules.flush();
        materialize(rule, effectiveFrom, effectiveFrom.plusDays(31));
        rule.advance(effectiveFrom.plusDays(32));
        return series.saveAndFlush(rule);
    }
    private void materialize(MedicationSeries s,LocalDate from,LocalDate through) {
        if(!s.isActive()) return;
        LocalDate start=from.isBefore(s.getStartDate())?s.getStartDate():from;
        LocalDate end=s.getEndDate()!=null && s.getEndDate().isBefore(through)?s.getEndDate():through;
        if(end.isBefore(start)) return;
        Set<LocalDate> existing=new HashSet<>(schedules.occurrenceDates(s.getId(),start,end));
        for(LocalDate day=start;!day.isAfter(end);day=day.plusDays(1)) {
            if(existing.contains(day) || !s.includes(day)) continue;
            var instant=day.atTime(s.getTimeOfDay()).atZone(ZoneId.of(s.getTimeZone())).toInstant();
            var row=Schedule.create(s.getPatientId(),null,ScheduleType.MEDICATION,s.getTitle(),instant);
            row.update(s.getTitle(),ScheduleType.MEDICATION,instant,s.getNotes()); row.assignSeries(s.getId(),day); schedules.save(row);
        }
        schedules.flush();
    }
    private static void requireWeekdays(int weekdays) {
        if (weekdays < 1 || weekdays > 127) throw invalid();
    }
    private static BusinessException invalid() { return new BusinessException(GlobalErrorCode.INVALID_REQUEST); }
}
