package com.coworking.space.urlservice;

import com.coworking.space.urlservice.clients.ModerationServiceClient;
import com.coworking.space.urlservice.dto.requests.CreateRandomUrlRequest;
import com.coworking.space.urlservice.dto.responses.UrlResponse;
import com.coworking.space.urlservice.utils.ModerationGateway;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Test
    void getById_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/urls/{id}", Integer.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirect_unknownCode_returnsNotFound() throws Exception {
        mockMvc.perform(get("/{code}", "nope404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsCreatedUrls() throws Exception {
        createUrl("https://example.com/list-1");
        createUrl("https://example.com/list-2");

        mockMvc.perform(get(URL_ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].longUrl",
                        hasItems("https://example.com/list-1", "https://example.com/list-2")));
    }

    @Test
    void updateUrl_changesLongUrl_keepsShortCode() throws Exception {
        UrlResponse created = createUrl("https://example.com/before");
        String code = codeOf(created);

        mockMvc.perform(put("/urls/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"longUrl": "https://example.com/after"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.longUrl").value("https://example.com/after"))
                .andExpect(jsonPath("$.shortUrl").value(created.shortUrl()));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/after"));
    }

    @Test
    void updateUrl_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(put("/urls/{id}", Integer.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"longUrl": "https://example.com/after"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUrl_invalidBody_returnsBadRequest() throws Exception {
        UrlResponse created = createUrl("https://example.com/to-update");

        mockMvc.perform(put("/urls/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"longUrl": "not a url"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.longUrl").exists());

        mockMvc.perform(get("/urls/{id}", created.id()))
                .andExpect(jsonPath("$.longUrl").value("https://example.com/to-update"));
    }

    @Test
    void deleteUrl_removesEntryAndRedirect() throws Exception {
        UrlResponse created = createUrl("https://example.com/to-delete");
        String code = codeOf(created);

        mockMvc.perform(delete("/urls/{id}", created.id()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/urls/{id}", created.id()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUrl_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/urls/{id}", Integer.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    private UrlResponse createUrl(String longUrl) throws Exception {
        MvcResult result = mockMvc.perform(post(URL_ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateRandomUrlRequest(longUrl))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(), UrlResponse.class);
    }

    private static String codeOf(UrlResponse response) {
        return response.shortUrl().substring(response.shortUrl().lastIndexOf('/') + 1);
    }
}
