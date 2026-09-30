package com.coworking.space.urlservice.services.implementation;

import com.coworking.space.urlservice.domain.entities.UrlEntity;
import com.coworking.space.urlservice.domain.excaptions.LimitTokenAttemptException;
import com.coworking.space.urlservice.domain.excaptions.UrlAlreadyExistsException;
import com.coworking.space.urlservice.domain.excaptions.UrlNotFoundException;
import com.coworking.space.urlservice.dto.requests.CreateCustomUrlRequest;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import com.coworking.space.urlservice.infrastructure.properties.UrlServiceProperties;
import com.coworking.space.urlservice.mappers.UrlMapper;
import com.coworking.space.urlservice.repositories.UrlRepository;
import com.coworking.space.urlservice.services.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepo;
    private final UrlMapper urlMapper;
    private final UrlServiceProperties urlServiceProperties;

    private static final int MAX_ATTEMPT_TO_CREATE = 1000;
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String LONG_URL_NOT_FOUND = "long url not found";
    private static final String ATTEMPT_LIMIT_OF_TOKEN_CREATION = "the limit of attempts to create a token has been exceeded";
    private static final String SHORT_URL_ALREADY_EXISTS = "short url already exists";

    @Override
    public UrlResponse createShortUrl(String longUrl) {
        var entity = urlRepo.findByLongUrl(longUrl);

        if(entity.isPresent()) {
            return urlMapper.toResponse(entity.get());
        }

        String shortUrl = generateShortToken(urlServiceProperties.getShortUrlLength());
        var urlEntity = UrlEntity.builder()
                .longUrl(longUrl)
                .shortUrl(shortUrl)
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
        if(urlRepo.existsByShortUrl(request.getCustomShortUrl())) {
            throw new UrlAlreadyExistsException(SHORT_URL_ALREADY_EXISTS);
        }

        var urlEntity = UrlEntity.builder()
                .longUrl(request.getLongUrl())
                .shortUrl(request.getCustomShortUrl())
                .build();

        urlRepo.save(urlEntity);
        return urlMapper.toResponse(urlEntity);
    }

    private String generateShortToken(int length) {
        StringBuilder token  = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for(int i = 0; i < MAX_ATTEMPT_TO_CREATE; i++) {
            for(int j = 0; j < length; j++) {
                int randomIndex = random.nextInt(BASE62.length());
                token.append(randomIndex);
            }

            String newShortUrl = token.toString();
            if(!urlRepo.existsByShortUrl(newShortUrl)) {
                return newShortUrl;
            }
        }

        throw new LimitTokenAttemptException(ATTEMPT_LIMIT_OF_TOKEN_CREATION);
    }
}
