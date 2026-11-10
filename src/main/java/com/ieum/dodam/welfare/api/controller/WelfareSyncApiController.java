package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.command.application.WelfareSyncService;
import com.ieum.dodam.welfare.infrastructure.gov24.Gov24WelfareProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/welfare-sync")
public class WelfareSyncApiController {
    private final WelfareSyncService syncService;
    private final Gov24WelfareProperties properties;

    public WelfareSyncApiController(WelfareSyncService syncService, Gov24WelfareProperties properties) {
        this.syncService = syncService;
        this.properties = properties;
    }

    @PostMapping
    public SyncResponse synchronize() {
        if (!properties.isSyncEnabled() || properties.getServiceKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "정부24 복지 API 동기화가 비활성화되어 있습니다.");
        }
        return new SyncResponse(syncService.synchronizeAll(properties.getPageSize()));
    }

    public record SyncResponse(int saved) {}
}
