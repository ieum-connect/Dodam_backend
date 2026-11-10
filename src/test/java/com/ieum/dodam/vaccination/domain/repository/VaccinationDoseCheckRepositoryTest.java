package com.ieum.dodam.vaccination.domain.repository;

import com.ieum.dodam.vaccination.domain.entity.VaccinationDoseCheck;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class VaccinationDoseCheckRepositoryTest {

    @Autowired VaccinationDoseCheckRepository checks;

    @Test
    void samePatientAndDoseCannotBeStoredTwice() {
        UUID patientId = UUID.randomUUID();
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "DTAP-1", null));

        assertThatThrownBy(() -> checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "DTAP-1", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listsOnlyThatPatientsDoseIdsInOrderAndDeletes() {
        UUID patientId = UUID.randomUUID();
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "HEPB-2", null));
        checks.saveAndFlush(VaccinationDoseCheck.create(patientId, "DTAP-1", null));
        checks.saveAndFlush(VaccinationDoseCheck.create(UUID.randomUUID(), "BCG-1", null));

        assertThat(checks.findDoseIds(patientId)).containsExactly("DTAP-1", "HEPB-2");
        assertThat(checks.existsByPatientIdAndDoseId(patientId, "DTAP-1")).isTrue();

        assertThat(checks.deleteByPatientIdAndDoseId(patientId, "DTAP-1")).isEqualTo(1);
        assertThat(checks.deleteByPatientIdAndDoseId(patientId, "DTAP-1")).isZero();
        assertThat(checks.findDoseIds(patientId)).containsExactly("HEPB-2");
    }
}
