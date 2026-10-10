# Portfolio Pilot — Backend

Java 21 + Spring Boot 3.2.5 · Spring Security (JWT + Google OIDC) · Spring Data JPA ·
BCrypt · TiDB Cloud (MySQL-compatible, sole database).

> Split from the complete project history
> ([shb09/portfolio-pilot](https://github.com/shb09/portfolio-pilot)).
> Frontend lives in [shb09/portfolio-pilot-frontend](https://github.com/shb09/portfolio-pilot-frontend).

## Run locally (Java 21)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
APP_AUTH_DEV_MODE=true mvn spring-boot:run   # :8080
```

TiDB + JWT + mail + OAuth resolve from env (`TIDB_URL`, `TIDB_USER`, `TIDB_PASSWORD`,
`JWT_SECRET`, `SPRING_MAIL_*`, `GOOGLE_OAUTH_CLIENT_ID/SECRET`, `APP_BASE_URL`,
`APP_CORS_ALLOWED_ORIGINS`). No secrets are committed — see `docs/deployment.md`
for the full variable table, Render settings, and the admin bootstrap/rotation procedure.
