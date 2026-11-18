package com.ieum.dodam.notification.push;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Instant;
@Component @ConditionalOnProperty(name="dodam.push.enabled",havingValue="true")
public class PushJob {
    private final PushService service;
    public PushJob(PushService service) { this.service=service; }
    @Scheduled(fixedDelayString="${dodam.push.delay-ms:5000}",initialDelay=20000)
    public void run() { for(int i=0;i<20 && service.deliverOne(Instant.now());i++) { /* bounded batch */ } }
}
