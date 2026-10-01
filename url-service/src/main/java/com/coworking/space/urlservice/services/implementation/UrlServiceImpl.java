package com.coworking.space.urlservice.services.implementation;

import com.coworking.space.urlservice.domain.entities.UrlEntity;
import com.coworking.space.urlservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.urlservice.dto.responses.ShortCodeCheckStatus;
import com.coworking.space.urlservice.infrastructure.excaptions.LimitTokenAttemptException;
import com.coworking.space.urlservice.infrastructure.excaptions.NotValidCustomCodeException;
import com.coworking.space.urlservice.infrastructure.excaptions.UrlAlreadyExistsException;
import com.coworking.space.urlservice.infrastructure.excaptions.UrlNotFoundException;
import com.coworking.space.urlservice.dto.requests.CreateCustomUrlRequest;
import com.coworking.space.urlservice.dto.requests.CreateRandomUrlRequest;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import com.coworking.space.urlservice.infrastructure.properties.UrlProperties;
import com.coworking.space.urlservice.mappers.UrlMapper;
import com.coworking.space.urlservice.repositories.UrlRepository;
import com.coworking.space.urlservice.services.UrlService;
import com.coworking.space.urlservice.utils.ModerationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepo;
    private final UrlMapper urlMapper;
    private final UrlProperties urlServiceProperties;
    private final ModerationGateway moderationGateway;

    private static final int MAX_ATTEMPT_TO_CREATE = 1000;
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String LONG_URL_NOT_FOUND = "long url not found";
    private static final String ATTEMPT_LIMIT_OF_TOKEN_CREATION = "the limit of attempts to create a token has been exceeded";
    private static final String SHORT_CODE_ALREADY_EXISTS = "short code already exists";
    private static final String CUSTOM_CODE_CONTAINS_BANNED_WORDS = "custom code contains banned words";

    @Override
    public UrlResponse createShortUrl(CreateRandomUrlRequest request) {
        var entity = urlRepo.findByLongUrl(request.getLongUrl());

        if(entity.isPresent()) {
            return urlMapper.toResponse(entity.get());
        }

        String shortCode = generateShortToken(urlServiceProperties.getShortUrlLength());
        var urlEntity = UrlEntity.builder()
                .longUrl(request.getLongUrl())
                .shortCode(shortCode)
                .build();

        urlRepo.save(urlEntity);
        return urlMapper.toResponse(urlEntity);
    }

    @Override
    public UrlResponse findById(int id) {
        var entity = urlRepo.findById(id)
                .orElseThrow(() -> new UrlNotFoundException(LONG_URL_NOT_FOUND));
        return urlMapper.toResponse(entity);
    }

    @Override
    public UrlResponse createCustomShortUrl(CreateCustomUrlRequest request) {
        if(urlRepo.existsByShortCode(request.getCustomShortCode())) {
            throw new UrlAlreadyExistsException(SHORT_CODE_ALREADY_EXISTS);
        }

        var moderationResponse = moderationGateway.check(
                new CheckShortCodeRequest(request.getCustomShortCode()));

        if(moderationResponse.getStatus() == ShortCodeCheckStatus.NOT_VALID) {
            throw new NotValidCustomCodeException(CUSTOM_CODE_CONTAINS_BANNED_WORDS);
        }

        var urlEntity = UrlEntity.builder()
                .longUrl(request.getLongUrl())
                .shortCode(request.getCustomShortCode())
                .build();

        urlRepo.save(urlEntity);
        return urlMapper.toResponse(urlEntity);
    }

    @Override
    public String getLongUrl(String shortCode) {
        var entity = urlRepo.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(SHORT_CODE_ALREADY_EXISTS));

        return entity.getLongUrl();
    }

    private String generateShortToken(int length) {
        StringBuilder token  = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for(int i = 0; i < MAX_ATTEMPT_TO_CREATE; i++) {
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
