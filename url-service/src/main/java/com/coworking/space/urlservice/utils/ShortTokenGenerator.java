package com.coworking.space.urlservice.utils;

import com.coworking.space.urlservice.infrastructure.excaptions.LimitTokenAttemptException;
import com.coworking.space.urlservice.repositories.UrlRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class ShortTokenGenerator {
    private final UrlRepository urlRepo;

    private static final int MAX_ATTEMPT_TO_CREATE = 1000;
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String ATTEMPT_LIMIT_OF_TOKEN_CREATION = "the limit of attempts to create a token has been exceeded";

    @Timed(
            value = "url.token.generation.duration",
            description = "Time to generate a unique short token",
            histogram = true
    )
    public String generateShortToken(int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for(int i = 0; i < MAX_ATTEMPT_TO_CREATE; i++) {
            StringBuilder token  = new StringBuilder(length);
            for(int j = 0; j < length; j++) {
                int randomIndex = random.nextInt(BASE62.length());
                token.append(BASE62.charAt(randomIndex));
            }

            String shortCode = token.toString();
            if(!urlRepo.existsByShortCode(shortCode)) {
                return shortCode;
            }
        }

        throw new LimitTokenAttemptException(ATTEMPT_LIMIT_OF_TOKEN_CREATION);
    }
}
