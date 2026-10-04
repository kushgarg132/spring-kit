# Recipes

Working code taken from two retired apps (a voter-roll admin system and a
per-ward volunteer app, both Spring Boot 3 + React, decommissioned
2026-10-04). **Not compiled, not part of the kit jar.** Copy what a new
app needs, then fix the `app.*` imports and the package line. A recipe
moves into the kit proper only when a second app needs it.

## Backend

| Recipe | What it gives you | Gotchas learned the hard way |
|---|---|---|
| `refresh-cookie/` | Refresh token in an `HttpOnly` cookie scoped to `/api/v1/auth`, a JS-readable `XSRF-TOKEN` cookie for double-submit CSRF on the cookie-authenticated endpoints, and JSON 401/403 from the security filter chain | Use it for browser-only apps. Capacitor/mobile apps (RoutineOS) keep the refresh token in the response body instead. `@RestControllerAdvice` never sees filter-chain auth errors, so the entry point has to write the error envelope itself |
| `bootstrap-admin/` | Seeds the first super-admin from `ADMIN_BOOTSTRAP_*` env vars when the admin table is empty. Generates one-time passwords without look-alike characters | Logs a loud warning when nobody can sign in. After first login: rotate the password, unset the vars |
| `stomp-auth/` | STOMP over plain WebSocket with JWT auth on the **CONNECT frame** (browsers can't set headers on the WS upgrade) and per-topic authorization on SUBSCRIBE | Interceptors run outside the servlet filter chain, so set the `SecurityContext` yourself around permission checks, then clear it. Re-check the account is still active, as the HTTP filter does. Nginx: the WS location needs `Upgrade`/`Connection` headers (see `nginx-tls.md` on the VM) |
| `web-push/` | VAPID Web Push: subscription entity, a sender behind a `PushSender` seam for tests, fan-out that never throws | Send with `Encoding.AES128GCM`: the library default (AESGCM) gets a 403 from FCM. Register the BouncyCastle provider before loading keys. Prune subscriptions on 404/410/**403** (403 = stale VAPID key). `pom-dependencies.xml` pins jose4j, bcprov-jdk18on and httpclient, which web-push doesn't bring in at compile scope |
| `resource-access/` | Per-resource RBAC: `VIEW < ACTION < GRANT` with explicit ranks, a guard bean usable in `@PreAuthorize("@guard.canView(#id)")` | Always re-read the role from the DB, never from JWT claims. Explicit rank map, so reordering enum constants can't flip checks. A GRANT holder must not approve their own access request |

## Frontend

| Recipe | What it gives you | Gotchas |
|---|---|---|
| `ui/` | Tailwind + Radix kit: Button, Badge, Card, Input/SearchInput, Textarea, Select, Checkbox, Dialog, RowActionsMenu, Pagination, StatTile, StatBarList, EmptyState, Skeleton, Toaster, ThemeProvider/ThemeToggle, plus `tailwind-preset.js` (brand + fixed status palette) | Needs the preset for `brand-*`, `status-*`, `shadow-panel`. Status is never shown by color alone (icon + label). Use a sorted meter list, not a categorical chart, when there are dozens of groups |
| `api-client/` | axios client: access token **in memory only**, one shared in-flight refresh on parallel 401s, retry once, `onSessionExpired` hook, react-query defaults | Pairs with `refresh-cookie/`. Restores the session on reload by calling `/auth/refresh` once at startup |
| `web-push/` | `isPushSupported`, enable/disable hooks, service worker (`sw.js`) with click-to-focus | Unsubscribe any existing subscription before subscribing: Chrome silently reuses a stale one after a VAPID key rotation, and every send then gets a 403 |
| `realtime/` | Singleton `@stomp/stompjs` client and a `useTopic` hook | Keep **all** subscribers and re-run them on every (re)connect. A single `onConnect` overwritten per hook drops subscriptions after a reconnect. Refresh `connectHeaders` before connecting, since the token may have changed |
