package com.coworking.space.urlservice.services;

import com.coworking.space.urlservice.dto.requests.CreateCustomUrlRequest;
import com.coworking.space.urlservice.dto.requests.CreateRandomUrlRequest;
import com.coworking.space.urlservice.dto.requests.UpdateUrlRequest;
import com.coworking.space.urlservice.dto.responses.UrlResponse;

import java.util.List;

public interface UrlService {
    UrlResponse createShortUrl(CreateRandomUrlRequest longUrl);

    UrlResponse findById(int id);

    UrlResponse createCustomShortUrl(CreateCustomUrlRequest request);

    String getLongUrl(String shortCode);

    List<UrlResponse> findAll();

    UrlResponse update(int id, UpdateUrlRequest request);

    void delete(int id);
}
