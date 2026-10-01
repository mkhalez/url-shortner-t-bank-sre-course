package com.coworking.space.urlservice.infrastructure.configuration;

import com.coworking.space.urlservice.clients.ModerationServiceClient;
import com.coworking.space.urlservice.infrastructure.properties.ModerationClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientConfig {
    @Bean
    public ModerationServiceClient userServiceClient(ModerationClientProperties moderationClientProperties) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(moderationClientProperties.getConnectionTimeout()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(moderationClientProperties.getReadTimeout()));

        RestClient restClient = RestClient.builder()
                .baseUrl(moderationClientProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builderFor(adapter).build();

        return httpServiceProxyFactory.createClient(ModerationServiceClient.class);
    }
}
