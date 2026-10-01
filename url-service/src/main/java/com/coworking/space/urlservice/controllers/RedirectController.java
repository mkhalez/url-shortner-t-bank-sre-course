package com.coworking.space.urlservice.controllers;

import com.coworking.space.urlservice.services.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {
    private final UrlService urlService;

    @GetMapping("/{shortUrl:[a-zA-Z0-9_-]{5,10}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortUrl) {
        var longUrl = urlService.getLongUrl(shortUrl);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
