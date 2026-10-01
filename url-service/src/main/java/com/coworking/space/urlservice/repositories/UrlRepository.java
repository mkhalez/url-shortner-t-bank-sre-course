package com.coworking.space.urlservice.repositories;

import com.coworking.space.urlservice.domain.entities.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Integer> {
    Optional<UrlEntity> findByShortCode(String shortCode);

    Optional<UrlEntity> findByLongUrl(String longUrl);

    boolean existsByShortCode(String shortCode);
}
