package recipes.bootstrapadmin; // snapshot: adapt package + imports

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates one-time credentials for newly created admin accounts — shown once, never stored raw. */
@Component
public class PasswordGenerator {

    // Excludes visually ambiguous characters (0/O, 1/l/I) since this is meant to be read and typed once.
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
