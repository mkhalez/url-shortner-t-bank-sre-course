package com.coworking.space.moderationservice.infrastrucure.properies;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "app.moderation")
public record BannedWordsProperties(Set<String> bannedWords) {
    public BannedWordsProperties {
        bannedWords = bannedWords == null
                ? Set.of()
                : bannedWords.stream()
                .map(word -> word.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
