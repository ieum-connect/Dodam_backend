package com.gyote.medibridge.user.api.dto.request;

import com.gyote.medibridge.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleRequestTest {

    @Test
    void rejectsAdmin() {
        RoleRequest request = new RoleRequest();
        request.setRole("ADMIN");
        assertThatThrownBy(request::toUserRole).isInstanceOf(BusinessException.class);
    }
}
