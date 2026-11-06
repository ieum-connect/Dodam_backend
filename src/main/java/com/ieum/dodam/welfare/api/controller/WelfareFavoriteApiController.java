package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.domain.entity.WelfareFavorite;
import com.ieum.dodam.welfare.domain.repository.WelfareFavoriteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-favorites")
public class WelfareFavoriteApiController {
    private final WelfareFavoriteRepository favorites;

    public WelfareFavoriteApiController(WelfareFavoriteRepository favorites) {
        this.favorites = favorites;
    }

    @GetMapping
    public List<UUID> list(@RequestParam UUID patientId) {
        return favorites.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(WelfareFavorite::getProgramId)
                .toList();
    }

    @PostMapping
    public WelfareFavorite add(@RequestParam UUID patientId, @RequestParam UUID programId) {
        return favorites.findByPatientIdAndProgramId(patientId, programId)
                .orElseGet(() -> favorites.save(WelfareFavorite.create(patientId, programId)));
    }

    @DeleteMapping("/{programId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID programId, @RequestParam UUID patientId) {
        favorites.findByPatientIdAndProgramId(patientId, programId).ifPresent(favorites::delete);
    }
}
