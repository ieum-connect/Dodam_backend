package com.ieum.medibridge.user.api.dto.request;

import com.ieum.medibridge.user.domain.UserRole;
import com.ieum.medibridge.global.exception.BusinessException;
import com.ieum.medibridge.user.error.UserErrorCode;

public class DemoLoginRequest {

    private String role;
    private String account;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

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
        if (chosen != UserRole.CAREGIVER) {
            throw new BusinessException(UserErrorCode.INVALID_ROLE);
        }
        return chosen;
    }
}
