package com.ieum.dodam.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import java.io.*;

@Configuration
@ConditionalOnProperty(name="dodam.push.enabled", havingValue="true")
public class FirebasePushConfiguration {
    @Bean(destroyMethod="delete")
    FirebaseApp pushFirebaseApp(@Value("${dodam.push.credentials}") String path) throws IOException {
        try(var input=new FileInputStream(path)) {
            return FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(input))
                .setConnectTimeout(5000).setReadTimeout(10000).build(), "dodam-push");
        }
    }
    @Bean PushSender firebasePushSender(FirebaseApp app) {
        var messaging=FirebaseMessaging.getInstance(app);
        return (token,reminder) -> {
            try {
                var message = Message.builder().setToken(token)
                    .putData("notificationId",reminder.notificationId().toString())
                    .putData("kind","dodam-reminder")
                    .putData("scheduleType",reminder.scheduleType().name())
                    .setWebpushConfig(WebpushConfig.builder()
                        .putHeader("TTL", "900")
                        .putHeader("Urgency", "high")
                        .build());
                if (reminder.title() != null) message.putData("title", reminder.title());
                messaging.send(message.build());
                return PushSender.Result.SENT;
            } catch(FirebaseMessagingException e) {
                // Never log tokens, payloads or SDK exception messages.
                return e.getMessagingErrorCode()==MessagingErrorCode.UNREGISTERED
                    ? PushSender.Result.INVALID_TOKEN : PushSender.Result.RETRY;
            }
        };
    }
}
