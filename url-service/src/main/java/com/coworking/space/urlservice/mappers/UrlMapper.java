package com.coworking.space.urlservice.mappers;

import com.coworking.space.urlservice.domain.entities.UrlEntity;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UrlMapper {
    UrlResponse toResponse(UrlEntity entity);
}
