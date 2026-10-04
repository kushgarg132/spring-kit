package io.github.kushgarg132.kit.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.github.kushgarg132.kit.error.InvalidCredentialsException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;

/**
 * Verifies Google Sign-In ID tokens (signature, issuer, expiry, audience). Needs
 * {@code com.google.api-client:google-api-client} on the app's classpath.
 */
public class GoogleIdTokens {

    private static final String INVALID = "Invalid or expired Google sign-in token";

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokens(Collection<String> clientIds) {
        this(new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build());
    }

    public GoogleIdTokens(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    /** @throws InvalidCredentialsException for any token that doesn't verify. */
    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new InvalidCredentialsException(INVALID);
            }
            return token.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new InvalidCredentialsException(INVALID);
        }
    }
}
