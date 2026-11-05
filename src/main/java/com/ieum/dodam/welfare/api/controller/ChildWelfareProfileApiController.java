package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.domain.entity.ChildWelfareProfile;
import com.ieum.dodam.welfare.domain.repository.ChildWelfareProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-profiles")
public class ChildWelfareProfileApiController {
    private final ChildWelfareProfileRepository profiles;
    public ChildWelfareProfileApiController(ChildWelfareProfileRepository profiles) { this.profiles = profiles; }

    @GetMapping
    public ProfileResponse get(@RequestParam UUID patientId) {
        return ProfileResponse.from(profiles.findByPatientId(patientId).orElseThrow(() -> new IllegalArgumentException("복지 프로필이 없습니다.")));
    }

    @PutMapping("/{patientId}")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse save(@PathVariable UUID patientId, @RequestBody ChildWelfareProfile.WelfareProfileCommand command) {
        Instant now = Instant.now();
        ChildWelfareProfile profile = profiles.findByPatientId(patientId).orElseGet(() -> ChildWelfareProfile.create(patientId, command, now));
        if (profile.getPatientId().equals(patientId) && profile.getProfileVersion() > 1) profile.update(command, now);
        return ProfileResponse.from(profiles.save(profile));
    }

    @DeleteMapping("/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable UUID patientId) {
        profiles.findByPatientId(patientId).ifPresent(profile -> { profile.withdraw(Instant.now()); profiles.save(profile); });
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
}
