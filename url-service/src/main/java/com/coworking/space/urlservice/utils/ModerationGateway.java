package com.coworking.space.urlservice.utils;

import com.coworking.space.urlservice.clients.ModerationServiceClient;
import com.coworking.space.urlservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.urlservice.dto.responses.CheckShortCodeResponse;
import com.coworking.space.urlservice.infrastructure.excaptions.ModerationIntegrationException;
import com.coworking.space.urlservice.infrastructure.excaptions.ModerationUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModerationGateway {
    private final ModerationServiceClient client;

    public CheckShortCodeResponse check(CheckShortCodeRequest request) {
        try {
            return client.isAllowed(request);
        } catch (ResourceAccessException | HttpServerErrorException e) {
            log.error("Moderation service is unavailable", e);
            throw new ModerationUnavailableException("Moderation service is unavailable", e);
        } catch (RestClientException e) {
            log.error("Unexpected response from moderation service", e);
            throw new ModerationIntegrationException("Unexpected moderation response", e);
        }
    }
}
