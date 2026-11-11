# 02 - Registry domain and REST API

## What was built

The four business tables (`api`, `consumer`, `subscription`, `api_key`) in Flyway migration `V2`, and one package per feature with controller, service, repository, entity and DTO records. All 16 endpoints from section 7 exist, errors are RFC 9457 problem details, lists are paged, and springdoc serves the OpenAPI document. A test exports it to `docs/openapi.json` and fails when the committed copy is stale; CI has a matching `contract` job. `local/demo.sh` runs the end-to-end scenario with `curl`. There is no authentication yet (milestone 4) and no Kafka (milestone 3).

## The three most important decisions

1. **Features talk through services and in-process Spring events, not through each other's entities.**
   Subscriptions need APIs and consumers, but "retiring an API cancels its subscriptions" needs the opposite direction, which would be a dependency cycle (Spring refuses to build one, and section 11 forbids it). `ApiService` therefore publishes an `ApiRetired` event and `SubscriptionService` listens; the same pattern links `SubscriptionCancelled` to key revocation. The listener is synchronous, so it runs inside the publisher's transaction and "both happen in the same transaction" holds. Entities and repositories are package-private, and a subscription stores `apiId` and `consumerId` as plain UUIDs, so the compiler enforces the boundary.
   Rejected: JPA `@ManyToOne` relations across features, and services calling each other in both directions.
   Production teams: would likely do the same inside a modular monolith, and publish the cascade as a durable event once features become separate services.

2. **Optimistic concurrency is checked twice.**
   `@Version` stops two simultaneous requests from overwriting each other, but it cannot notice that the client's copy was already stale when the request arrived, so the service also compares the `rowVersion` the client sent. Both paths return 409. A unique constraint violation is also mapped to 409, because the database is the final arbiter when two requests race.
   Rejected: relying on `@Version` alone (it would silently accept a stale client), or locking rows on every read.
   Production teams: often use ETags and `If-Match` headers for the same purpose.

3. **API keys are stored as plain SHA-256 plus a lookup prefix.**
   A key is 256 random bits, so there is nothing to guess: an attacker who steals the table cannot brute-force a 2^256 space, and a slow hash would only add CPU cost to every gateway call. A password is different: it has little entropy, so a fast hash can be guessed offline, which is why passwords need bcrypt or Argon2. The 8-character prefix lets validation find candidate rows through an index, and hashes are then compared with `MessageDigest.isEqual`, which takes the same time whether or not they match. Validation always answers HTTP 200, so a caller cannot tell a wrong key from a missing one by status code.
   Rejected: bcrypt for keys (cost without benefit) and storing keys encrypted (a second secret to protect).
   Production teams: might add an HMAC with a server-side pepper held in a KMS.

## Where the specification was silent (my assumptions)

- An API moves **one step at a time**; skipping `DRAFT` to `DEPRECATED` is rejected.
- Validation accepts keys for `PUBLISHED` and `DEPRECATED` APIs; `DRAFT` or `RETIRED` gives `API_NOT_AVAILABLE`. In practice retiring revokes the keys first, so that reason is rare.
- `rowVersion` is required on every update and status change, for all three updatable resources.
- Issuing a key accepts an optional `expiresAt` (otherwise the `EXPIRED` reason could never occur).
- Revoking an already revoked key returns 204 again, so retries are safe.
- Suspending a consumer does not suspend its subscriptions; the spec does not say so.
- The `(api_id, consumer_id)` uniqueness means a cancelled subscription cannot be re-created.
- A successful validation writes `last_used_at`, so validation is a write.

## Known limitations

- Two concurrent requests could both pass the "at most two active keys" check; closing that needs a row lock or a database constraint.
- Every unique-constraint violation is reported as the same generic 409 message.
- Subscriptions are not re-checked when a suspended consumer is reactivated, and key listing is not paged.

## Acceptance results

- `./mvnw verify`: 27 unit and web tests and 36 integration tests passed, BUILD SUCCESS.
- `local/demo.sh` completed against the running service: valid before revocation, `REVOKED` after.
- Swagger UI served (HTTP 200); the live `/v3/api-docs` lists 16 operations.
- GitHub Actions: not yet run; it needs the owner to push.
