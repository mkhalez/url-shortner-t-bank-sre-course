package com.coworking.space.urlservice.controllers;

import com.coworking.space.urlservice.dto.requests.CreateCustomUrlRequest;
import com.coworking.space.urlservice.dto.requests.CreateRandomUrlRequest;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import com.coworking.space.urlservice.services.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
public class UrlController {
    private final UrlService urlService;

    private static final String LOCATION_OF_CREATED_RESOURCE_PATTERN = "/urls/{id}";

    @PostMapping
    public ResponseEntity<UrlResponse> createUrl(@RequestBody CreateRandomUrlRequest request, UriComponentsBuilder builder) {
        var response = urlService.createShortUrl(request);
        var location = builder.path(LOCATION_OF_CREATED_RESOURCE_PATTERN).buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UrlResponse> findUrlById(@PathVariable int id) {
        var response = urlService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/custom")
    public ResponseEntity<UrlResponse> createCustomUrl(@RequestBody CreateCustomUrlRequest request, UriComponentsBuilder builder) {
        var response = urlService.createCustomShortUrl(request);
        var location = builder.path(LOCATION_OF_CREATED_RESOURCE_PATTERN).buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location)
                .body(response);
    }






















}
