package com.ieum.dodam.user.api.dto.response;

import com.ieum.dodam.user.domain.UserStatus;

/** 탈퇴 처리 결과다. */
public record WithdrawalResponse(UserStatus userStatus, int revokedRelationCount) {
}
