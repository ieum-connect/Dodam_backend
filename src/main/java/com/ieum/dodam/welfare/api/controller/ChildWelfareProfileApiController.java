package com.ieum.dodam.welfare.api.controller;

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
        return new ProfileOptionsResponse(List.of(
                new IncomeBandOption("BELOW_50", "기준중위소득 50% 이하", 50),
                new IncomeBandOption("BELOW_75", "기준중위소득 75% 이하", 75),
                new IncomeBandOption("BELOW_100", "기준중위소득 100% 이하", 100),
                new IncomeBandOption("BELOW_120", "기준중위소득 120% 이하", 120),
                new IncomeBandOption("BELOW_150", "기준중위소득 150% 이하", 150),
                new IncomeBandOption("ABOVE_150", "기준중위소득 150% 초과", null)
        ));
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
                           String regionCode, Integer householdSize, String incomeBand, Integer childCount,
                           Boolean singleParent, Boolean multicultural, Boolean disability, Boolean dualIncome,
                           String childcareStatus, boolean healthDataConsent, Instant updatedAt) {
        static ProfileResponse from(ChildWelfareProfile p) {
            return new ProfileResponse(p.getPatientId(), p.isStorageConsent(), p.getConsentedAt(), p.getProfileVersion(),
                    p.getRegionCode(), p.getHouseholdSize(), p.getIncomeBand(), p.getChildCount(), p.getSingleParent(),
                    p.getMulticultural(), p.getDisability(), p.getDualIncome(), p.getChildcareStatus(),
                    p.isHealthDataConsent(), p.getUpdatedAt());
        }
    }

    record ProfileOptionsResponse(List<IncomeBandOption> incomeBands) {}
    record IncomeBandOption(String value, String label, Integer maximumPercent) {}
}
