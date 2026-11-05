package com.ieum.dodam.welfare.api.controller;

import com.ieum.dodam.welfare.command.application.WelfareRecommendationService;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/welfare-recommendations")
public class WelfareRecommendationApiController {
    private final WelfareRecommendationService recommendations;
    public WelfareRecommendationApiController(WelfareRecommendationService recommendations) { this.recommendations = recommendations; }
    @GetMapping
    public WelfareRecommendationService.RecommendationResult list(@RequestParam UUID patientId,
                                                                    @RequestParam(defaultValue = "SUGGESTIVE") String type) {
        return recommendations.recommend(patientId, "PROACTIVE".equalsIgnoreCase(type));
    }
}
