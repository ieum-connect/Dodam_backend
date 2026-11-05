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
    public ChildWelfareProfile get(@RequestParam UUID patientId) {
        return profiles.findByPatientId(patientId).orElseThrow(() -> new IllegalArgumentException("복지 프로필이 없습니다."));
    }

    @PutMapping("/{patientId}")
    @ResponseStatus(HttpStatus.OK)
    public ChildWelfareProfile save(@PathVariable UUID patientId, @RequestBody ChildWelfareProfile.WelfareProfileCommand command) {
        Instant now = Instant.now();
        ChildWelfareProfile profile = profiles.findByPatientId(patientId).orElseGet(() -> ChildWelfareProfile.create(patientId, command, now));
        if (profile.getPatientId().equals(patientId) && profile.getProfileVersion() > 1) profile.update(command, now);
        return profiles.save(profile);
    }

    @DeleteMapping("/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable UUID patientId) {
        profiles.findByPatientId(patientId).ifPresent(profile -> { profile.withdraw(Instant.now()); profiles.save(profile); });
    }
}
