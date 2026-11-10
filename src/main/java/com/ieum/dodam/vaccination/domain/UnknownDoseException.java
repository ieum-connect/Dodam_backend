package com.ieum.dodam.vaccination.domain;

import com.ieum.dodam.global.exception.BusinessException;
import com.ieum.dodam.vaccination.error.VaccinationErrorCode;

import java.util.List;

/** AI가 completedDoses 중 모르는 차수가 있다고 거절했습니다. 모르는 차수 목록은 비어 있을 수 있습니다. */
public class UnknownDoseException extends BusinessException {

    private final List<String> unknownDoseIds;

    public UnknownDoseException(List<String> unknownDoseIds) {
        super(VaccinationErrorCode.UNKNOWN_DOSE);
        this.unknownDoseIds = List.copyOf(unknownDoseIds);
    }

    public List<String> unknownDoseIds() {
        return unknownDoseIds;
    }
}
