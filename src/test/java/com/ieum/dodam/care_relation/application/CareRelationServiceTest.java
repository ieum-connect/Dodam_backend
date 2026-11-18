package com.ieum.dodam.care_relation.application;

import com.ieum.dodam.care_relation.command.application.CareRelationCommandService;
import com.ieum.dodam.care_relation.domain.CareRelationCode;
import com.ieum.dodam.care_relation.domain.CareRelation;
import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.care_relation.error.CareRelationErrorCode;
import com.ieum.dodam.care_relation.query.application.CareRelationQueryService;
import com.ieum.dodam.care_relation.query.model.CareRelationView;
import com.ieum.dodam.user.command.application.UserAccountService;
import com.ieum.dodam.user.query.application.UserQueryService;
import com.ieum.dodam.user.command.application.UserWithdrawalService;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.user.domain.UserStatus;
import com.ieum.dodam.user.error.UserErrorCode;
import com.ieum.dodam.user.domain.repository.UserRepository;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CareRelationServiceTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private CareRelationRepository relations;

    @Autowired
    private PatientRepository patients;

    @Test
    void caregiverRequestsWithPatientInviteCode() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );

        String inviteCode = accounts.patientInviteCode(patient);
        CareRelation created = cares.request(caregiver, CareRelationCode.display(inviteCode));

        assertThat(created.getStatus()).isEqualTo(CareRelationStatus.REQUESTED);
        assertThat(created.getPatientId()).isEqualTo(patients.findByUserId(patient.getId()).orElseThrow().getId());
        assertThatThrownBy(() -> cares.request(caregiver, inviteCode))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patientAcceptsThenCaregiverCannotAccept() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));

        CareRelation accepted = cares.accept(patient, requested.getId());

        assertThat(accepted.getStatus()).isEqualTo(CareRelationStatus.ACTIVE);
        assertThatThrownBy(() -> cares.accept(caregiver, requested.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownOrSelfCodeIsRejected() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );

        assertThatThrownBy(() -> cares.request(caregiver, "AAAAAA"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void withdrawnPatientInviteCodeIsRejected() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        UserWithdrawalService withdrawals = new UserWithdrawalService(users, patients, relations,
                org.mockito.Mockito.mock(com.ieum.dodam.global.auth.oauth.KakaoUnlinkClient.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        String inviteCode = accounts.patientInviteCode(patient);

        withdrawals.withdraw(patient.getKakaoId(), true);

        assertThatThrownBy(() -> cares.request(caregiver, inviteCode))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.INVITE_CODE_NOT_FOUND);
        assertThat(relations.findByCaregiverIdOrderByRequestedAtDesc(caregiver.getId())).isEmpty();
    }

    @Test
    void caregiverWithdrawnAfterAuthenticationCannotRequest() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        caregiver.setStatus(UserStatus.WITHDRAWN);

        assertThatThrownBy(() -> cares.request(caregiver, accounts.patientInviteCode(patient)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.USER_ALREADY_WITHDRAWN);
        assertThat(relations.findByCaregiverIdOrderByRequestedAtDesc(caregiver.getId())).isEmpty();
    }

    @Test
    void acceptIsRejectedWhenCaregiverWithdrewBeforeLockWasAcquired() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));
        caregiver.setStatus(UserStatus.WITHDRAWN);

        assertThatThrownBy(() -> cares.accept(patient, requested.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CareRelationErrorCode.INVALID_RELATION_STATE);
        assertThat(requested.getStatus()).isEqualTo(CareRelationStatus.REQUESTED);
    }

    @Test
    void eitherSideCanRevokeActiveLink() {
        UserAccountService accounts = new UserAccountService(users, patients);
        CareRelationCommandService cares = new CareRelationCommandService(relations, patients, users, org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
        User patient = accounts.chooseRole(
                accounts.loginOrRegister("kakao-soonja", "김순자").getKakaoId(),
                UserRole.PATIENT
        );
        User caregiver = accounts.chooseRole(
                accounts.loginOrRegister("kakao-minji", "김민지").getKakaoId(),
                UserRole.CAREGIVER
        );
        CareRelation requested = cares.request(caregiver, accounts.patientInviteCode(patient));
        cares.accept(patient, requested.getId());

        CareRelation revoked = cares.revoke(patient, requested.getId());

        assertThat(revoked.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThatThrownBy(() -> cares.revoke(caregiver, requested.getId()))
                .isInstanceOf(BusinessException.class);
    }

}
