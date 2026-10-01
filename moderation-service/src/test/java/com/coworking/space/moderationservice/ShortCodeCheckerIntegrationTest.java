package com.coworking.space.moderationservice;

import com.coworking.space.moderationservice.dto.requests.CheckShortCodeRequest;
import com.coworking.space.moderationservice.dto.responses.CheckShortCodeResponse;
import com.coworking.space.moderationservice.dto.responses.ShortCodeCheckStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ShortCodeCheckerIntegrationTest {
    private static final String SHORT_CODE_ROUTE = "/shortcode";

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shortCodeWithBanWordTest() throws Exception {
        String shortCode = "bandcjsdcj";
        CheckShortCodeRequest request = new CheckShortCodeRequest(shortCode);
        CheckShortCodeResponse expected = new CheckShortCodeResponse(shortCode, ShortCodeCheckStatus.NOT_VALID);

        String responseJson = mockMvc.perform(post(SHORT_CODE_ROUTE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        CheckShortCodeResponse actual = mapper.readValue(responseJson, CheckShortCodeResponse.class);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shortCodeWithoutBanWordTest() throws Exception {
        String shortCode = "sdcdsc";
        CheckShortCodeRequest request = new CheckShortCodeRequest(shortCode);
        CheckShortCodeResponse expected = new CheckShortCodeResponse(shortCode, ShortCodeCheckStatus.VALID);

        String responseJson = mockMvc.perform(post(SHORT_CODE_ROUTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        CheckShortCodeResponse actual = mapper.readValue(responseJson, CheckShortCodeResponse.class);

        Assertions.assertEquals(expected, actual);
    }
}
