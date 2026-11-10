package com.ieum.dodam.notification.push;
import java.util.UUID;
public interface PushSender {
    enum Result { SENT, INVALID_TOKEN, RETRY }
    Result send(String token, UUID notificationId);
}
