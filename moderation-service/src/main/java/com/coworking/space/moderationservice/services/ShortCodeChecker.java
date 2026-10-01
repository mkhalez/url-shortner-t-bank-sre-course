package com.coworking.space.moderationservice.services;

import com.coworking.space.moderationservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.moderationservice.dto.responses.CheckShortCodeResponse;

public interface ShortCodeChecker {
    CheckShortCodeResponse isAllowed(CheckShortCodeRequest request);
}
