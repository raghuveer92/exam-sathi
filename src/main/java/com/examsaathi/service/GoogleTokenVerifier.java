package com.examsaathi.service;

import com.examsaathi.config.GoogleOAuthProperties;
import com.examsaathi.exception.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleIdToken.Payload verify(String idTokenString) {
        var clientIds = googleOAuthProperties.acceptedClientIds();
        if (clientIds.isEmpty()) {
            throw new BadRequestException("Google Sign-In is not configured on the server");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BadRequestException("Invalid Google idToken");
            }
            return idToken.getPayload();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to verify Google idToken: " + ex.getMessage());
        }
    }
}
