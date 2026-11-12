package com.wellwithlife.omsbackend.controller;

import com.wellwithlife.omsbackend.dto.GlobalSearchResponse;
import com.wellwithlife.omsbackend.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping("/global")
    public ResponseEntity<GlobalSearchResponse> globalSearch(
            @RequestParam(value = "q", defaultValue = "") String query,
            Authentication authentication) {
        return ResponseEntity.ok(globalSearchService.executeSearch(query, authentication));
    }
}