package com.gyote.medibridge.user.api.dto.response;

import com.gyote.medibridge.user.domain.UserStatus;

/** 탈퇴 처리 결과다. */
public record WithdrawalResponse(UserStatus userStatus, int revokedRelationCount) {
}
