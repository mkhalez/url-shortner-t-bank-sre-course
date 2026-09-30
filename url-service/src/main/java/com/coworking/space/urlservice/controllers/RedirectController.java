package com.coworking.space.urlservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {
    @GetMapping("/{code:[a-zA-Z0-9_-]{5,10}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {

    }
}
