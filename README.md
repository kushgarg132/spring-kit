# spring-kit

Shared Spring Boot 3 building blocks for kushgarg132's backends. It's a small
library: only the error handler auto-registers. Everything else is a plain
class the app builds from its own properties.

| Class | What |
|---|---|
| `error.*` + `GlobalExceptionHandler` | `ApiException` hierarchy → uniform `ApiErrorResponse` JSON. Auto-registered at lowest precedence; `kit.error-handler.enabled=false` turns it off |
| `web.ApiResponse`, `web.PageResponse` | Success envelopes |
| `web.ClientIp` | Real client IP behind Nginx (`X-Real-IP`, never spoofable `X-Forwarded-For`) |
| `security.JwtCodec` | HMAC JWT issue/parse; compatible with the apps' older providers given the same secret |
| `security.BearerTokenFilter` | `Authorization: Bearer` → `Authentication` via your mapping function |
| `security.AttemptLimiter` | In-memory sliding-window failed-attempt limiter (single instance) |
| `security.OpaqueTokens` | Refresh/reset tokens: generate + SHA-256 hash |
| `security.GoogleIdTokens` | Google Sign-In ID token verification (needs `google-api-client`) |
| `crypto.AesGcmCipher` | AES-256-GCM for secrets at rest |

## Use

```xml
<repositories>
  <repository><id>jitpack.io</id><url>https://jitpack.io</url></repository>
</repositories>

<dependency>
  <groupId>com.github.kushgarg132</groupId>
  <artifactId>spring-kit</artifactId>
  <version>v0.1.1</version>
</dependency>
```

Gradle: `maven { url 'https://jitpack.io' }` and
`implementation 'com.github.kushgarg132:spring-kit:v0.1.1'`.

```java
@Bean JwtCodec jwt(MyJwtProperties p) {
    return new JwtCodec(p.secret(), Duration.ofMinutes(p.accessTokenTtlMinutes()));
}

@Bean SecurityFilterChain chain(HttpSecurity http, JwtCodec jwt) throws Exception {
    var bearer = new BearerTokenFilter(jwt, claims -> Optional.of(
            new UsernamePasswordAuthenticationToken(UUID.fromString(claims.getSubject()), null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")))));
    return http.addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class)/* ... */.build();
}
```

Release: bump `<version>`, `git tag vX.Y.Z && git push --tags`. JitPack builds on first request.

## Recipes

`recipes/` holds snapshots of code from retired apps (refresh cookie + CSRF, STOMP JWT auth, Web Push, per-resource RBAC, a React UI kit, an axios client that refreshes on 401). They are not compiled. See `recipes/README.md`.
