package com.coworking.space.urlservice.mappers;

import com.coworking.space.urlservice.domain.entities.UrlEntity;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import com.coworking.space.urlservice.utils.ShortUrlFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ShortUrlFormatter.class)
public interface UrlMapper {

    @Mapping(target = "shortUrl", source = "shortCode", qualifiedByName = "toFullUrl")
    UrlResponse toResponse(UrlEntity entity);
}
