package com.gyote.medibridge.user.api.dto.request;

import com.gyote.medibridge.user.domain.UserRole;
import com.gyote.medibridge.global.exception.BusinessException;
import com.gyote.medibridge.user.error.UserErrorCode;

public class RoleRequest {

    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UserRole toUserRole() {
        if (role == null || role.isBlank()) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
        UserRole chosen;
        try {
            chosen = UserRole.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
        if (chosen != UserRole.PATIENT && chosen != UserRole.CAREGIVER) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
        return chosen;
    }
}
