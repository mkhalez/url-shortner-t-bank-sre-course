package com.coworking.space.moderationservice;

import com.coworking.space.moderationservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.moderationservice.dto.responses.CheckShortCodeResponse;
import com.coworking.space.moderationservice.dto.responses.ShortCodeCheckStatus;
import com.coworking.space.moderationservice.infrastrucure.properies.BannedWordsProperties;
import com.coworking.space.moderationservice.services.ShortCodeChecker;
import com.coworking.space.moderationservice.services.implementation.ShortCodeCheckerImpl;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeCheckerTest {
    private final ShortCodeChecker checker = new ShortCodeCheckerImpl(
            new BannedWordsProperties(Set.of("ban", "spam", "java")));

    @Test
    void shortCodeWithBanWordsTest() {
        String shortCode = "bansdcdsc";
        CheckShortCodeRequest request = new CheckShortCodeRequest(shortCode);
        CheckShortCodeResponse expected = new CheckShortCodeResponse(shortCode, ShortCodeCheckStatus.NOT_VALID);

        var actual = checker.isAllowed(request);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shortCodeWithoutBanWordsTest() {
        String shortCode = "sdcdsc";
        CheckShortCodeRequest request = new CheckShortCodeRequest(shortCode);
        CheckShortCodeResponse expected = new CheckShortCodeResponse(shortCode, ShortCodeCheckStatus.VALID);

        var actual = checker.isAllowed(request);

        assertThat(actual).isEqualTo(expected);
    }
}
