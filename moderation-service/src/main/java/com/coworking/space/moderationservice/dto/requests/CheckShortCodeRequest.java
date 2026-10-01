package com.coworking.space.moderationservice.dto.requests;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Getter
public class CheckShortCodeRequest {
    private String shortCode;
}
