package com.ieum.dodam.notification.push;

import com.ieum.dodam.user.query.application.UserQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import com.ieum.dodam.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@RestController @RequestMapping("/api/push")
public class PushApiController {
    private final PushDeviceRepository devices;
    private final UserQueryService users;
    @Value("${dodam.push.enabled:false}") private boolean enabled;
    @Value("${dodam.push.web-api-key:}") private String apiKey;
    @Value("${dodam.push.project-id:}") private String projectId;
    @Value("${dodam.push.sender-id:}") private String senderId;
    @Value("${dodam.push.app-id:}") private String appId;
    @Value("${dodam.push.vapid-key:}") private String vapidKey;
    public PushApiController(PushDeviceRepository devices,UserQueryService users) { this.devices=devices; this.users=users; }
    public record DeviceRequest(@NotBlank @Size(max=4096) String token, Boolean showDetails) {}
    @GetMapping("/config") public Map<String,Object> config() {
        return Map.of("enabled",enabled,"apiKey",apiKey,"projectId",projectId,"messagingSenderId",senderId,"appId",appId,"vapidKey",vapidKey);
    }
    @PostMapping("/devices") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void register(@AuthenticationPrincipal OAuth2User principal,@Valid @RequestBody DeviceRequest body) {
        if(!enabled) throw new BusinessException(PushErrorCode.PUSH_DISABLED);
        var userId=actor(principal); var id=hash(body.token());
        var device=devices.findById(id).orElseGet(()->new PushDevice(id,userId,body.token()));
        device.bind(userId,body.token());
        if (body.showDetails() != null) device.changeDetailsVisibility(body.showDetails());
        devices.save(device);
    }
    @DeleteMapping("/devices") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void unregister(@AuthenticationPrincipal OAuth2User principal,@Valid @RequestBody DeviceRequest body) {
        var userId=actor(principal);
        devices.findById(hash(body.token())).filter(d->d.getUserId().equals(userId)).ifPresent(PushDevice::disable);
    }
    private UUID actor(OAuth2User principal) { Object key=principal.getAttribute("id"); return users.requireByKakaoId(String.valueOf(key)).getId(); }
    static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
