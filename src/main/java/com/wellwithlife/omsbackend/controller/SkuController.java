package com.wellwithlife.omsbackend.controller;

import com.wellwithlife.omsbackend.entity.SkuMaster;
import com.wellwithlife.omsbackend.repository.SkuMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

    @RestController
    @RequestMapping("/api/skus")
    @RequiredArgsConstructor
    public class SkuController {

        private final SkuMasterRepository skuRepo;

        @GetMapping("/active")
        public ResponseEntity<List<SkuMaster>> getActiveSkus() {
            return ResponseEntity.ok(skuRepo.findByIsActiveTrueOrderByProductNameAsc());
        }
    }

