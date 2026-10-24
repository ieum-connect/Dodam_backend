package com.gyote.medibridge.user.api.dto.request;

import com.gyote.medibridge.user.domain.UserRole;
import com.gyote.medibridge.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleRequestTest {

    @Test
    void acceptsPatientOrCaregiver() {
        RoleRequest request = new RoleRequest();
        request.setRole("PATIENT");
        assertThat(request.toUserRole()).isEqualTo(UserRole.PATIENT);
        request.setRole("CAREGIVER");
        assertThat(request.toUserRole()).isEqualTo(UserRole.CAREGIVER);
    }

    @Test
    void rejectsAdmin() {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");
        assertThatThrownBy(request::toUserRole).isInstanceOf(BusinessException.class);
    }
}
