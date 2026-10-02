package com.coworking.space.urlservice.services.implementation;

import com.coworking.space.urlservice.domain.entities.UrlEntity;
import com.coworking.space.urlservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.urlservice.dto.requests.UpdateUrlRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
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

        log.atInfo()
                .addKeyValue("event", "createShortUrl")
                .addKeyValue("long_url", request.getLongUrl())
                .addKeyValue("short_code", urlEntity.getShortCode())
                .log();
        return urlMapper.toResponse(urlEntity);
    }

    @Override
    public UrlResponse findById(int id) {
        var entity = urlRepo.findById(id)
                .orElseThrow(() -> new UrlNotFoundException(LONG_URL_NOT_FOUND));

        log.atInfo()
                .addKeyValue("event", "findById")
                .addKeyValue("entity_id", id)
                .log();
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

        log.atInfo()
                .addKeyValue("event", "createCustomShortUrl")
                .addKeyValue("long_url", request.getLongUrl())
                .addKeyValue("short_code", urlEntity.getShortCode())
                .log();

        return urlMapper.toResponse(urlEntity);
    }

    @Override
    public String getLongUrl(String shortCode) {
        var entity = urlRepo.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(SHORT_CODE_ALREADY_EXISTS));

        log.atInfo()
                .addKeyValue("event", "getLongUrl")
                .addKeyValue("short_code", shortCode)
                .addKeyValue("long_url", entity.getLongUrl())
                .log();

        return entity.getLongUrl();
    }

    @Override
    public List<UrlResponse> findAll() {
        return urlRepo.findAll().stream().map(urlMapper::toResponse).toList();
    }

    @Override
    public UrlResponse update(int id, UpdateUrlRequest request) {
        var entity = urlRepo.findById(id)
                .orElseThrow(() -> new UrlNotFoundException(LONG_URL_NOT_FOUND));
        entity.setLongUrl(request.getLongUrl());
        urlRepo.save(entity);

        log.atInfo()
                .addKeyValue("event", "update")
                .addKeyValue("long_url", request.getLongUrl())
                .addKeyValue("new_short_code", entity.getShortCode())
                .log();

        return urlMapper.toResponse(entity);
    }

    @Override
    public void delete(int id) {
        if (!urlRepo.existsById(id)) {
            throw new UrlNotFoundException(LONG_URL_NOT_FOUND);
        }
        urlRepo.deleteById(id);

        log.atInfo()
                .addKeyValue("event", "delete")
                .addKeyValue("id", id)
                .log();
    }

    private String generateShortToken(int length) {
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
