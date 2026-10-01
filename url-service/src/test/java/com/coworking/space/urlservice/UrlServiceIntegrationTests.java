package com.coworking.space.urlservice;

import com.coworking.space.urlservice.clients.ModerationServiceClient;
import com.coworking.space.urlservice.dto.requests.CreateRandomUrlRequest;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import com.coworking.space.urlservice.utils.ModerationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UrlServiceIntegrationTests {
    private static final String URL_ROUTE = "/urls";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ModerationGateway moderationGateway;

    @MockitoBean
    private ModerationServiceClient moderationServiceClient;


    @Test
    void createUrl_createsUsableShortUrl() throws Exception {
        var request = new CreateRandomUrlRequest("https://google.com");

        MvcResult result = mockMvc.perform(post(URL_ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/urls/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.longUrl").value("https://google.com"))
                .andExpect(jsonPath("$.shortUrl").value(startsWith("http://localhost:8080/")))
                .andReturn();

        UrlResponse created = mapper.readValue(
                result.getResponse().getContentAsString(), UrlResponse.class);
        String code = created.shortUrl().substring(created.shortUrl().lastIndexOf('/') + 1);

        assertThat(code).matches("[a-zA-Z0-9_-]{5,10}");

        mockMvc.perform(get("/urls/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longUrl").value("https://google.com"));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    void createUrl_blankLongUrl_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL_ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"longUrl": ""}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.longUrl").exists());
    }

    @Test
    void createUrl_malformedLongUrl_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL_ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"longUrl": "not a url"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.longUrl").exists());
    }
}
