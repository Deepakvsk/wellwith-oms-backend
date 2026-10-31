package com.wellwithlife.omsbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wellwithlife.omsbackend.service.EasyecomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ProductController {

    private final EasyecomService easyecomService;

    @GetMapping("/easyecom")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'BDE')")
    public ResponseEntity<JsonNode> getEasyecomProducts() {
        log.info("Fetching EasyEcom catalog products...");
        return ResponseEntity.ok(easyecomService.fetchProductMaster());
    }
}