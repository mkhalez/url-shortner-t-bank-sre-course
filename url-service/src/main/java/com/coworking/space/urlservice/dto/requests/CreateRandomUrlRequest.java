package com.coworking.space.urlservice.dto.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@NoArgsConstructor
@Getter
@AllArgsConstructor
public class CreateRandomUrlRequest {
    @NotBlank(message = "long url must be not blank")
    @URL(message = "long url must be url")
    private String longUrl;
}
