package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.domain.WelfareIncomeBand;
import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.user.domain.UserStatus;
import com.ieum.dodam.user.domain.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-profiles")
public class ChildWelfareProfileApiController {
    private final ChildWelfareProfileRepository profiles;
    private final CareRelationRepository relations;
    private final UserRepository users;
    public ChildWelfareProfileApiController(ChildWelfareProfileRepository profiles, CareRelationRepository relations, UserRepository users) { this.profiles = profiles; this.relations = relations; this.users = users; }

    @GetMapping("/options")
    public ProfileOptionsResponse options() {
        // 소득구간 정의는 추천 엔진과 같은 열거형을 공유한다.
        return new ProfileOptionsResponse(java.util.Arrays.stream(WelfareIncomeBand.values())
                .map(band -> new IncomeBandOption(band.name(), band.label(), band.upperInclusivePercent()))
                .toList(), List.of(
                new ChildcareOption("가정양육", "가정양육"),
                new ChildcareOption("어린이집 이용", "어린이집 이용"),
                new ChildcareOption("유치원 이용", "유치원 이용"),
                new ChildcareOption("학교 재학", "학교 재학"),
                new ChildcareOption("기타", "기타")
        ),
                new IncomeGuide(2026, List.of(2564238, 4199292, 5359036, 6494738, 7556719, 8555952, 9515150),
                        "https://www.mohw.go.kr/menu.es?mid=a10708010900"));
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal OAuth2User principal, @RequestParam UUID patientId) {
        authorize(principal, patientId);
        return ProfileResponse.from(profiles.findByPatientId(patientId).orElseThrow(() -> new IllegalArgumentException("복지 프로필이 없습니다.")));
    }

    @PutMapping("/{patientId}")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse save(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID patientId, @RequestBody ChildWelfareProfile.WelfareProfileCommand command) {
        authorize(principal, patientId);
        Instant now = Instant.now();
        ChildWelfareProfile profile = profiles.findByPatientId(patientId).orElseGet(() -> ChildWelfareProfile.create(patientId, command, now));
        if (profile.getPatientId().equals(patientId)) profile.update(command, now);
        return ProfileResponse.from(profiles.save(profile));
    }

    @DeleteMapping("/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID patientId) {
        authorize(principal, patientId);
        profiles.findByPatientId(patientId).ifPresent(profile -> { profile.withdraw(Instant.now()); profiles.save(profile); });
    }

    private void authorize(OAuth2User principal, UUID patientId) {
        String kakaoId = principal == null ? null : String.valueOf(principal.getAttributes().get("id"));
        User user = users.findByKakaoId(kakaoId).filter(u -> u.getRole() == UserRole.CAREGIVER && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));
        if (!relations.existsByPatientIdAndCaregiverIdAndStatus(patientId, user.getId(), CareRelationStatus.ACTIVE))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "연결된 아이의 복지 프로필만 조회할 수 있습니다.");
    }

    record ProfileResponse(UUID patientId, boolean storageConsent, Instant consentedAt, long profileVersion,
                           String regionCode, String regionName, Integer householdSize, String incomeBand, Integer childCount,
                           Boolean singleParent, Boolean multicultural, Boolean disability, Boolean dualIncome,
                           String childcareStatus, boolean healthDataConsent, Instant updatedAt) {
        static ProfileResponse from(ChildWelfareProfile p) {
            return new ProfileResponse(p.getPatientId(), p.isStorageConsent(), p.getConsentedAt(), p.getProfileVersion(),
                    p.getRegionCode(), p.getRegionName(), p.getHouseholdSize(), p.getIncomeBand(), p.getChildCount(), p.getSingleParent(),
                    p.getMulticultural(), p.getDisability(), p.getDualIncome(), p.getChildcareStatus(),
                    p.isHealthDataConsent(), p.getUpdatedAt());
        }
    }

    record ProfileOptionsResponse(List<IncomeBandOption> incomeBands, List<ChildcareOption> childcareOptions, IncomeGuide incomeGuide) {}
    record IncomeBandOption(String value, String label, Integer maximumPercent) {}
    record ChildcareOption(String value, String label) {}
    record IncomeGuide(int year, List<Integer> medianMonthlyIncomeByHouseholdSize, String sourceUrl) {}
}
