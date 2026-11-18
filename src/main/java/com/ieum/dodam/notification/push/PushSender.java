package com.ieum.dodam.notification.push;

import com.ieum.dodam.schedule.domain.ScheduleType;
import java.util.UUID;

public interface PushSender {
    enum Result { SENT, INVALID_TOKEN, RETRY }
    record Reminder(UUID notificationId, ScheduleType scheduleType, String title) {}
    Result send(String token, Reminder reminder);
}
