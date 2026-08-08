package com.noahgeerts.progress.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noahgeerts.progress.domain.PerformedSet.CreatePerformedSetDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSetResponseDto;
import com.noahgeerts.progress.domain.PerformedSet.UpdatePerformedSetDto;
import com.noahgeerts.progress.service.PerformedSetService;

@RestController
@RequestMapping("/sets")
public class PerformedSetController {

    private PerformedSetService setService;

    public PerformedSetController(PerformedSetService setService) {
        this.setService = setService;
    }

    @PostMapping("")
    public ResponseEntity<PerformedSetResponseDto> createPerformedSet(@AuthenticationPrincipal Jwt jwt,
            @Validated @RequestBody CreatePerformedSetDto dto) {
        PerformedSetResponseDto created = setService.
        createPerformedSet(jwt.getSubject(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{stid}")
    public ResponseEntity<PerformedSetResponseDto> updatePerformedSet(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long stid, @Validated @RequestBody UpdatePerformedSetDto dto) {
        PerformedSetResponseDto updated = setService.updatePerformedSet(jwt.getSubject(), stid, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{stid}")
    public ResponseEntity<Void> deletePerformedSet(@AuthenticationPrincipal Jwt jwt, @PathVariable Long stid) {
        setService.deletePerformedSet(jwt.getSubject(), stid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
