package com.ieum.dodam.welfare.command.application;

import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.entity.WelfareProgram;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import com.ieum.dodam.welfare.domain.repository.WelfareProgramRepository;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class WelfareRecommendationService {
    private final PatientRepository patients; private final ChildWelfareProfileRepository profiles; private final WelfareProgramRepository programs; private final Clock clock;
    public WelfareRecommendationService(PatientRepository patients, ChildWelfareProfileRepository profiles, WelfareProgramRepository programs) { this(patients, profiles, programs, Clock.systemUTC()); }
    WelfareRecommendationService(PatientRepository p, ChildWelfareProfileRepository f, WelfareProgramRepository g, Clock c) { patients=p; profiles=f; programs=g; clock=c; }
    public RecommendationResult recommend(UUID patientId, boolean proactive) {
        Patient child = patients.findById(patientId).orElseThrow(() -> new IllegalArgumentException("아이를 찾을 수 없습니다."));
        ChildWelfareProfile profile = profiles.findByPatientId(patientId).orElse(null);
        if (proactive && (profile == null || !profile.isStorageConsent())) return new RecommendationResult(true, List.of());
        int months = child.getBirthDate() == null ? -1 : Period.between(child.getBirthDate(), LocalDate.now(clock)).getMonths() + Period.between(child.getBirthDate(), LocalDate.now(clock)).getYears() * 12;
        if (months < 0 || months > 95) return new RecommendationResult(false, List.of());
        return new RecommendationResult(false, programs.findByActiveTrueOrderByModifiedAtSourceDescIdDesc().stream().map(p -> RecommendationItem.of(p, score(p, profile))).sorted((a,b)->Double.compare(b.score(), a.score())).limit(50).toList());
    }
    private double score(WelfareProgram p, ChildWelfareProfile profile) { String text=(p.getName()+" "+p.getTargetText()+" "+p.getPurposeSummary()).toLowerCase(); double score=30; if(text.contains("영아")||text.contains("유아")||text.contains("아동")||text.contains("보육")) score+=30; if(profile!=null&&profile.getRegionCode()!=null&&text.contains(profile.getRegionCode().toLowerCase())) score+=20; if(p.getModifiedAtSource()!=null) score+=5; return Math.min(score,100); }
    public record RecommendationResult(boolean profileRequired, List<RecommendationItem> items) {}
    public record RecommendationItem(UUID programId, String name, double score, String status) { static RecommendationItem of(WelfareProgram p,double s){ return new RecommendationItem(p.getId(),p.getName(),s,s>=70?"ELIGIBLE":"POSSIBLE"); } }
}
