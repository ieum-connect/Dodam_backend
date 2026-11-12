package com.ieum.dodam.user.command.application;

import com.ieum.dodam.care_relation.domain.CareRelation;
import com.ieum.dodam.care_relation.domain.CareRelationStatus;
import com.ieum.dodam.care_relation.domain.repository.CareRelationRepository;
import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.global.auth.oauth.KakaoUnlinkClient;
import com.ieum.dodam.patient.domain.Patient;
import com.ieum.dodam.patient.domain.repository.PatientRepository;
import com.ieum.dodam.user.api.dto.response.WithdrawalResponse;
import com.ieum.dodam.user.domain.User;
import com.ieum.dodam.user.domain.UserRole;
import com.ieum.dodam.user.domain.UserStatus;
import com.ieum.dodam.user.domain.repository.UserRepository;
import com.ieum.dodam.user.error.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.transaction.TestTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@Import(UserWithdrawalService.class)
class UserWithdrawalServiceTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private CareRelationRepository relations;

    @Autowired
    private UserWithdrawalService withdrawals;

    @MockBean
    private KakaoUnlinkClient kakao;

    @Test
    void patientWithdrawalRevokesActiveAndCancelsRequestedRelationsUsingPatientProfileId() {
        User patientUser = saveUser("patient-kakao", UserRole.PATIENT);
        Patient patient = savePatient(patientUser, "ABC123");
        User caregiver = saveUser("caregiver-kakao", UserRole.CAREGIVER);
        CareRelation active = saveRelation(patient.getId(), caregiver, CareRelationStatus.ACTIVE);
        CareRelation requested = saveRelation(patient.getId(), saveUser("other-kakao", UserRole.CAREGIVER), CareRelationStatus.REQUESTED);

        WithdrawalResponse response = withdrawals.withdraw(patientUser.getKakaoId(), true);

        verify(kakao).unlink("patient-kakao");
        assertThat(response.userStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(response.revokedRelationCount()).isEqualTo(1);
        assertThat(patientUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(patientUser.getDeletedAt()).isNotNull();
        assertThat(active.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThat(active.getEndedAt()).isNotNull();
        assertThat(requested.getStatus()).isEqualTo(CareRelationStatus.CANCELED);
        assertThat(requested.getEndedAt()).isNotNull();
    }

    @Test
    void caregiverWithdrawalRevokesActiveAndCancelsRequestedRelations() {
        User patientUser = saveUser("patient-kakao", UserRole.PATIENT);
        Patient patient = savePatient(patientUser, "ABC123");
        Patient otherPatient = savePatient(saveUser("other-patient-kakao", UserRole.PATIENT), "DEF456");
        User caregiver = saveUser("caregiver-kakao", UserRole.CAREGIVER);
        CareRelation active = saveRelation(patient.getId(), caregiver, CareRelationStatus.ACTIVE);
        CareRelation requested = saveRelation(otherPatient.getId(), caregiver, CareRelationStatus.REQUESTED);
        CareRelation othersActive = saveRelation(patient.getId(), saveUser("other-caregiver-kakao", UserRole.CAREGIVER), CareRelationStatus.ACTIVE);

        WithdrawalResponse response = withdrawals.withdraw(caregiver.getKakaoId(), true);

        assertThat(response.revokedRelationCount()).isEqualTo(1);
        assertThat(caregiver.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(active.getStatus()).isEqualTo(CareRelationStatus.REVOKED);
        assertThat(requested.getStatus()).isEqualTo(CareRelationStatus.CANCELED);
        assertThat(othersActive.getStatus()).isEqualTo(CareRelationStatus.ACTIVE);
    }

    @Test
    void withdrawalRequiresExplicitConfirmation() {
        User user = saveUser("patient-kakao", UserRole.PATIENT);

        assertThatThrownBy(() -> withdrawals.withdraw(user.getKakaoId(), false))
                .isInstanceOf(BusinessException.class);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verifyNoInteractions(kakao);
    }

    @Test
    void providerFailureRollsBackWithdrawal() {
        User user = saveUser("unlink-failure", UserRole.CAREGIVER);
        var id = user.getId();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        doThrow(new BusinessException(UserErrorCode.KAKAO_UNLINK_FAILED)).when(kakao).unlink("unlink-failure");
        try {
            assertThatThrownBy(() -> withdrawals.withdraw("unlink-failure", true))
                    .isInstanceOf(BusinessException.class);
            User restored = users.findById(id).orElseThrow();
            assertThat(restored.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(restored.getKakaoId()).isEqualTo("unlink-failure");
        } finally {
            users.deleteById(id);
        }
    }

    @Test
    void demoWithdrawalDoesNotCallKakao() {
        User user = saveUser("demo-caregiver", UserRole.CAREGIVER);
        assertThat(withdrawals.withdraw(user.getKakaoId(), true).userStatus()).isEqualTo(UserStatus.WITHDRAWN);
        verifyNoInteractions(kakao);
    }

    private User saveUser(String kakaoId, UserRole role) {
        User user = new User();
        user.setKakaoId(kakaoId);
        user.setName(kakaoId);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    private Patient savePatient(User user, String inviteCode) {
        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setInviteCode(inviteCode);
        return patients.saveAndFlush(patient);
    }

    private CareRelation saveRelation(java.util.UUID patientId, User caregiver, CareRelationStatus status) {
        CareRelation relation = new CareRelation();
        relation.setPatientId(patientId);
        relation.setCaregiverId(caregiver.getId());
        relation.setStatus(status);
        return relations.saveAndFlush(relation);
    }
}
