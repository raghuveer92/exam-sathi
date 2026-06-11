package com.examsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleSignInRequest {

    @NotBlank(message = "Google idToken is required")
    private String idToken;
}
