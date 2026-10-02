package com.coworking.space.moderationservice.services.implementation;

import com.coworking.space.moderationservice.dto.responses.ShortCodeCheckStatus;
import com.coworking.space.moderationservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.moderationservice.dto.responses.CheckShortCodeResponse;
import com.coworking.space.moderationservice.infrastrucure.properies.BannedWordsProperties;
import com.coworking.space.moderationservice.services.ShortCodeChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortCodeCheckerImpl implements ShortCodeChecker {
    private final BannedWordsProperties bannedWordsProperties;

    @Override
    public CheckShortCodeResponse isAllowed(CheckShortCodeRequest request) {
        String normalizedCode = request.getShortCode().toLowerCase(Locale.ROOT);
        ShortCodeCheckStatus currentStatus = bannedWordsProperties.bannedWords()
                 .stream()
                 .anyMatch(normalizedCode::contains)
                 ? ShortCodeCheckStatus.NOT_VALID
                 : ShortCodeCheckStatus.VALID;

        log.atInfo()
                .addKeyValue("shortCode", request.getShortCode())
                .addKeyValue("is allowed", currentStatus)
                .log();

        return CheckShortCodeResponse.builder()
                .shortCode(request.getShortCode())
                .status(currentStatus)
                .build();
    }
}
