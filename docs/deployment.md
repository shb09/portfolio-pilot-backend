# Deployment Guide — Portfolio Pilot

## Architecture recap

```
Browser (React SPA)
  → Spring Boot API (JWT, :8080) → TiDB Cloud
```

## Required environment variables

### Backend

| Variable | Purpose | Example |
|---|---|---|
| `TIDB_URL` | JDBC URL (TLS on) | `jdbc:mysql://<host>:4000/portfolio_pilot?sslMode=VERIFY_IDENTITY&serverTimezone=UTC` |
| `TIDB_USER` / `TIDB_PASSWORD` | TiDB credentials | — |
| `JWT_SECRET` | HMAC key, **≥32 chars**, unique per env | `openssl rand -hex 48` |
| `APP_BASE_URL` | Public URL used in email links (**never localhost in prod**) | `https://app.example.com` |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | SMTP server | `smtp.gmail.com` / `587` |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | SMTP credentials (app password, not login) | — |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | SMTP auth flag | `true` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | STARTTLS flag | `true` |
| `APP_MAIL_FROM` | Sender shown to users | `Portfolio Pilot <no-reply@example.com>` |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | One-time admin bootstrap (blank password = skipped) | — |
| `APP_AUTH_DEV_MODE` | Local testing only (**must be `false`/unset in prod**) | `false` |
| `GOOGLE_OAUTH_CLIENT_ID` / `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth (absent = Google button hidden, password-only) | — |

### Frontend (build-time)

| Variable | Purpose |
|---|---|
| `VITE_API_URL` | Backend origin, e.g. `https://api.example.com` |
| `VITE_PUBLIC_URL` | Prefix for `/portfolio/*` if served separately (usually same as above or empty with proxy) |

Backend CORS: set `app.cors.allowed-origins` (currently only in code default `http://localhost:5173`;
pass `--app.cors.allowed-origins=https://app.example.com` or env if wired) to the frontend origin.

## Google OAuth setup (optional, password login always works)

Backend implements authorization-code + OIDC (`OAuthClientConfig`, `GoogleLinkingService`,
`HandoffStore`, `OAuthHandlers`); frontend has the Google button (hidden unless configured),
`/oauth/callback` exchange, and role-based redirect. Status: **implemented, tested with mocks,
NOT end-to-end tested — no Google credentials exist in this environment.**

1. [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials →
   Create Credentials → **OAuth client ID** → Application type **Web application**.
2. Under Authorized redirect URIs add exactly (scheme/host/port/path must match):
   - local: `http://localhost:8080/login/oauth2/code/google`
   - prod: `https://<api-host>/login/oauth2/code/google`
3. Configure the OAuth consent screen (app name, support email); while in Testing mode,
   add tester Gmail addresses under Test users.
4. Set backend env (never in code, never in the frontend):
   `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`.
5. Restart backend; `GET /api/auth/oauth/status` → `{"googleEnabled":true}`; the login page
   shows “Continue with Google”.
6. Test: click → Google consent → redirect to `/oauth/callback?code=…` → dashboard.
   Colliding password-account emails are rejected (409, keeps password login); ADMIN
   accounts must use password login; unverified Google emails are rejected.

## Deployment checklist

- [ ] TiDB database `portfolio_pilot` exists; app has least-privilege credentials.
- [ ] `JWT_SECRET` unique, ≥32 chars, stored in the platform secret manager.
- [ ] `APP_BASE_URL` is the real https origin (verification/reset links depend on it).
- [ ] SMTP configured + test registration delivers mail; `APP_AUTH_DEV_MODE` is **unset/false**.
- [ ] Google OAuth credentials set (or deliberately omitted — button hides itself); redirect URI registered exactly.
- [ ] Admin bootstrapped via env, then **rotate the initial password** and unset `APP_ADMIN_PASSWORD`.
- [ ] Frontend built with `VITE_API_URL`; SPA fallback (rewrite → `/index.html`) configured on the host.
- [ ] CORS allowlist = frontend origin only (no `*` with credentials).
- [ ] No `.env` files, keystores, or passwords committed (`.gitignore` covers `.env`).
- [ ] Logs contain no passwords/tokens (verified: grep-clean).
- [ ] Health check: `GET /api/health` (add `/api/health/db` for DB-aware probes).
- [ ] `ddl-auto=update` is a learning-mode convenience; adopt `validate` + reviewed migrations for prod.

## Local development

```bash
# backend (:8080) — dev mode enables verification testing without SMTP
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home  # Mockito needs JDK 21
APP_AUTH_DEV_MODE=true \
APP_ADMIN_EMAIL='<admin-email>' APP_ADMIN_USERNAME='admin' APP_ADMIN_PASSWORD='<admin-password>' \
mvn spring-boot:run

# frontend (:5173, /api proxied to :8080)
cd frontend && npm install && npm run dev
```

Test accounts (local TiDB): `admin`/`<admin-password>` (ADMIN, rotate before any public exposure),
`student@test.com`/`password123` (USER). Register → verify (dev token in response) → login.

## Admin password rotation (explicit local utility)

`AdminPasswordResetRunner` resets **only** the existing ADMIN account's `password_hash`
(BCrypt) and runs **only** when BOTH conditions hold — otherwise the boot is a no-op:

1. program argument `--reset-admin-password` is passed, and
2. env `APP_ADMIN_EMAIL` names the account + env `APP_ADMIN_NEW_PASSWORD` holds the new
   password (10–100 chars).

It refuses when the email is unset/unknown or the account is not ADMIN. Nothing is logged
except the username. The normal bootstrap never overwrites an existing password.

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export APP_ADMIN_EMAIL='shb64178@gmail.com'
export APP_ADMIN_NEW_PASSWORD='<new-strong-password>'   # 10+ chars, never commit
mvn spring-boot:run -Dspring-boot.run.arguments="--reset-admin-password"
# expect: "Admin password hash updated for username='admin'..."
unset APP_ADMIN_NEW_PASSWORD
```

Verify afterwards: (1) login as `admin` with the new password → 200 + `"role":"ADMIN"`;
(2) old password → 401; (3) `GET /api/auth/me` shows `"role":"ADMIN"`;
(4) `GET /api/admin/stats` with the new token → 200; (5) an ordinary user's token
on `/api/admin/*` → 403.

## Production topology: Vercel (frontend) + Render (backend) + TiDB Cloud

### Backend on Render — exact settings

- **Root directory:** `backend` · **Build command:** `mvn -q clean package -DskipTests`
  (or `./mvnw` equivalent; repo uses system Maven) · **Start command:**
  `java -jar target/portfolio-pilot-0.0.1-SNAPSHOT.jar`
- **Java version:** set `JAVA_VERSION=21` (Render env) so the toolchain matches `pom.xml`.
- **Port:** app reads `PORT` automatically (`server.port=${PORT:8080}`); no code change needed.
- **Health check path:** `/api/health` (public, no user data; `/api/health/db` also exists for DB-aware probes).
- **Environment variables (Render dashboard → Environment, never in Git):**
  `TIDB_URL`, `TIDB_USER`, `TIDB_PASSWORD`, `JWT_SECRET` (≥32 chars, fresh value),
  `APP_BASE_URL=https://<vercel-app>.vercel.app`, `APP_CORS_ALLOWED_ORIGINS=https://<vercel-app>.vercel.app`,
  SMTP vars (`SPRING_MAIL_*`, `APP_MAIL_FROM`), `GOOGLE_OAUTH_CLIENT_ID` /
  `GOOGLE_OAUTH_CLIENT_SECRET` (only if enabling Google login),
  **leave `APP_AUTH_DEV_MODE` unset**, **leave `APP_ADMIN_PASSWORD` unset**
  (bootstrap skips; admin already exists in TiDB).
- **Schema:** Hibernate `ddl-auto=update` applies additive changes (e.g. `google_sub` column);
  no destructive operation. Adopt `validate` + reviewed migrations afterwards.

### Frontend on Vercel — exact settings

- **Root directory:** `frontend` · **Framework preset:** Vite ·
  **Build command:** `npm run build` · **Output directory:** `dist`.
- **Environment variable:** `VITE_API_URL=https://<render-backend>.onrender.com`
  (this is the exact variable `src/api/client.js` reads; Login's Google button prefixes it too).
  No secrets in any `VITE_*` variable.
- **SPA rewrites:** already covered — `frontend/vercel.json` rewrites all routes to `/`
  and `frontend/public/_redirects` covers Netlify-style hosts.
- **Google OAuth production callback** (register after Render assigns the host):
  `https://<render-backend>.onrender.com/login/oauth2/code/google`
  Keep the localhost callback registered too for development.

## Known limitations

- No browser in this environment: pixel-level theme/layout checks need one real-browser pass.
- Expired-JWT rejection shares the tested tampered-token path (`JwtException` → 401); no dedicated
  expired-token live test was run (tokens live 24h).
- Rate limiter is in-memory (single instance); use a shared store when scaling horizontally.
- SMTP was unavailable here: verification/reset flows were tested end-to-end in dev mode
  (raw token in response); real delivery needs provider credentials per the table above.
