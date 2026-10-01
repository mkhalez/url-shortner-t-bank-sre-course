package com.coworking.space.urlservice.clients;

import com.coworking.space.urlservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.urlservice.dto.responses.CheckShortCodeResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface ModerationServiceClient {
    @PostExchange("/shortcode")
    CheckShortCodeResponse isAllowed(@RequestBody CheckShortCodeRequest request);
}
