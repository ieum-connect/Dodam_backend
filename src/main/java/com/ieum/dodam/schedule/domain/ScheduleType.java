package com.ieum.dodam.schedule.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ScheduleType {
    MEDICATION,
    REVISIT,
    TEST,
    PERSONAL,
    OTHER;

    @JsonCreator
    public static ScheduleType fromName(String name) {
        return valueOf(name);
    }
}
