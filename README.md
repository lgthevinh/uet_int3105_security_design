# uet_int3105

Microservices demo for INT3105 (Software Architecture) covering gateway security patterns: gateway routing, offloading (JWT check + rate limit), valet key and API aggregation.

## Architecture

```
client ──▶ gateway (nginx + njs, :8080)
              ├─ /api/v1/auth/*      ──▶ user-service  (public: register, login)
              ├─ /api/v1/users/*     ──▶ user-service  (JWT)
              ├─ /api/v1/orders/*    ──▶ order-service (JWT)
              ├─ /api/v1/dashboard   ──▶ njs: user-service + order-service (JWT)
              └─ /storage/*          ──▶ minio         (signed form / public read)
```

| Service       | Tech                  | Host port |
|---------------|-----------------------|-----------|
| gateway       | nginx + njs           | 8080      |
| user-service  | Spring Boot, Java 21  | 8081      |
| order-service | Spring Boot, Java 21  | 8082      |
| minio         | MinIO (S3 API)        | 9000      |
| minio         | MinIO (console)       | 9001      |

Data is in memory and resets when a service restarts. user-service and order-service are also exposed directly (8081, 8082) on purpose, to compare calls with and without the gateway.

## Run

```bash
make compose   # docker compose up -d --build
make down      # docker compose down
```

Set `JWT_SECRET` (at least 32 bytes) to override the dev default; it must be the same for the gateway and user-service. MinIO console: http://localhost:9001 (`minioadmin` / `minioadmin`).

Seeded users: `alice@example.com` and `bob@example.com`, password `password123`.

## Tasks

**Gateway routing** — one entry point at `:8080`, paths routed /user and /order service.

**Offloading** — implement JWT check with njs module and api rate limiting 10 request/seconds.

**Valet key** — upload user avatar via signed-url from user-service to minio object storage

**API aggregation** — `/api/v1/dashboard` aggregate user and user orders asynchronously with subrequest and build response with njs

## API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | – | Create user + credentials |
| POST | `/api/v1/auth/login` | – | Returns a JWT (1h) |
| GET | `/api/v1/users`, `/api/v1/users/{id}` | JWT | List / get users |
| POST | `/api/v1/users/me/avatar/upload-form` | JWT | Signed MinIO POST form for an avatar |
| PUT | `/api/v1/users/me/avatar` | JWT | Confirm upload, set `avatarUrl` |
| GET | `/api/v1/orders` (`?userId=`) | JWT | List |
| POST | `/api/v1/orders` | JWT | Create order (`userId` in body) |
| GET | `/api/v1/orders/{id}` | JWT | Get order |
| GET | `/api/v1/dashboard` | JWT | Caller's user + orders |
| GET | `/storage/avatars/{key}` | – | Public avatar image |
