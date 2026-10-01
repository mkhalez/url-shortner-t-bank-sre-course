package com.coworking.space.moderationservice.controllers;

import com.coworking.space.moderationservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.moderationservice.dto.responses.CheckShortCodeResponse;
import com.coworking.space.moderationservice.services.ShortCodeChecker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shortcode")
@RequiredArgsConstructor
public class CheckShortCodeController {
    private final ShortCodeChecker shortCodeChecker;

    @PostMapping
    public ResponseEntity<CheckShortCodeResponse> checkShortCode(@RequestBody @Valid CheckShortCodeRequest request) {
        var response= shortCodeChecker.isAllowed(request);
        return ResponseEntity.ok(response);
    }
}
