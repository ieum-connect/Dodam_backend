package com.ieum.dodam.user.api.dto.request;

import com.ieum.dodam.global.exception.BusinessException;
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
