package com.examsaathi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private String email;
    private String fullName;
    @Builder.Default
    private Boolean emailVerificationRequired = true;
}
