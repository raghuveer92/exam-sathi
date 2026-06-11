package com.examsaathi.dto.request;

import lombok.Data;

/**
 * Self-service account deletion — confirm with password (email accounts)
 * or a fresh Google idToken (Google Sign-In accounts).
 */
@Data
public class DeleteAccountRequest {

    /** Required for email/password accounts. */
    private String password;

    /** Required for Google-only accounts — re-authenticate via Google Sign-In. */
    private String idToken;
}
