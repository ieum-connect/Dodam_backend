package com.ieum.dodam.patient.api;

import com.ieum.dodam.care_relation.domain.CareRelation;
import com.ieum.dodam.care_relation.domain.CareRelationCode;
import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.user.domain.UserStatus;
import com.ieum.dodam.user.domain.repository.UserRepository;
import com.ieum.dodam.user.error.UserErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/children")
public class ChildApiController {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final PatientRepository patients;
    private final CareRelationRepository relations;
    private final UserRepository users;

    public ChildApiController(PatientRepository patients, CareRelationRepository relations, UserRepository users) {
        this.patients = patients;
        this.relations = relations;
        this.users = users;
    }

    public record ChildRequest(String name, LocalDate birthDate) {}
    public record JoinRequest(String inviteCode) {}
    public record ChildResponse(UUID id, String name, LocalDate birthDate, String inviteCode, int parentCount) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<ChildResponse> list(@AuthenticationPrincipal OAuth2User principal) {
        User parent = parent(principal);
        return relations.findByCaregiverIdAndStatus(parent.getId(), CareRelationStatus.ACTIVE).stream()
                .map(link -> patients.findById(link.getPatientId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(child -> child.getChildName() != null)
                .map(this::response).toList();
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public ChildResponse create(@AuthenticationPrincipal OAuth2User principal, @RequestBody ChildRequest request) {
        User parent = parent(principal);
        validate(request);
        Patient child = new Patient();
        child.setUserId(parent.getId());
        child.setChildName(request.name().trim());
        child.setBirthDate(request.birthDate());
        child.setInviteCode(newCode());
        child = patients.save(child);
        CareRelation link = new CareRelation();
        link.setPatientId(child.getId());
        link.setCaregiverId(parent.getId());
        link.setStatus(CareRelationStatus.ACTIVE);
        link.setAcceptedAt(Instant.now());
        relations.save(link);
        return response(child);
    }

    @PutMapping("/{id}")
    @Transactional
    public ChildResponse update(@AuthenticationPrincipal OAuth2User principal, @PathVariable UUID id,
                                @RequestBody ChildRequest request) {
        User parent = parent(principal);
        if (!relations.existsByPatientIdAndCaregiverIdAndStatus(id, parent.getId(), CareRelationStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "연결된 아이만 수정할 수 있습니다.");
        }
        validate(request);
        Patient child = patients.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이를 찾을 수 없습니다."));
        child.setChildName(request.name().trim());
        child.setBirthDate(request.birthDate());
        return response(child);
    }

    @PostMapping("/join")
    @Transactional
    public ChildResponse join(@AuthenticationPrincipal OAuth2User principal, @RequestBody JoinRequest request) {
        User parent = parent(principal);
        if (request == null || request.inviteCode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "초대 코드를 입력해 주세요.");
        }
        Patient child = patients.findByInviteCode(CareRelationCode.normalize(request.inviteCode()))
                .filter(p -> p.getChildName() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "아이를 찾을 수 없습니다."));
        // The creator's user row serializes joins and withdrawal against the same child.
        users.findAllByIdForUpdate(List.of(child.getUserId()));
        if (relations.existsByPatientIdAndCaregiverIdAndStatus(child.getId(), parent.getId(), CareRelationStatus.ACTIVE)) {
            return response(child);
        }
        long activeParents = relations.countByPatientIdAndStatus(child.getId(), CareRelationStatus.ACTIVE);
        if (activeParents == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "연결된 부모가 없어 초대 코드를 사용할 수 없습니다.");
        }
        if (activeParents >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이 아이에는 이미 부모 두 명이 연결되어 있습니다.");
        }
        CareRelation link = new CareRelation();
        link.setPatientId(child.getId());
        link.setCaregiverId(parent.getId());
        link.setStatus(CareRelationStatus.ACTIVE);
        link.setAcceptedAt(Instant.now());
        relations.save(link);
        return response(child);
    }

    private User parent(OAuth2User principal) {
        String kakaoId = principal == null ? null : String.valueOf(principal.getAttributes().get("id"));
        return users.findByKakaoId(kakaoId)
                .filter(u -> u.getRole() == UserRole.CAREGIVER && u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validate(ChildRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().trim().length() > 100 || request.birthDate() == null
                || request.birthDate().isAfter(LocalDate.now())
                || request.birthDate().isBefore(LocalDate.now().minusYears(19))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이 이름과 19세 미만의 생년월일을 확인해 주세요.");
        }
    }

    private ChildResponse response(Patient child) {
        return new ChildResponse(child.getId(), child.getChildName(), child.getBirthDate(),
                CareRelationCode.display(child.getInviteCode()),
                (int) relations.countByPatientIdAndStatus(child.getId(), CareRelationStatus.ACTIVE));
    }

    private String newCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < 8; i++) value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            if (patients.findByInviteCode(value.toString()).isEmpty()) return value.toString();
        }
        throw new BusinessException(UserErrorCode.INVITE_CODE_GENERATION_FAILED);
    }
}
