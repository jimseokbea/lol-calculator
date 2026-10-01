# Riot settlement review fixes

- Each user's markers, minutes and opportunity cost commit in one REQUIRES_NEW transaction. A Riot request or database write/commit failure rolls back that user; other users can still commit. This also applies when Spring Batch supplies an outer transaction. The user is reloaded inside the new transaction.
- ProcessedMatch uses the (userId, matchId) JPA composite identity. Repeated settlement skips only that user's markers; shared matches are credited independently.
- Settlement requests pages of 20 until a short/empty page. It scans past existing markers so older gaps are not hidden behind a processed boundary. IDs repeated between pages are counted once. The recent-five-game calculator keeps its existing first-page behavior.

## Existing MySQL database

Before starting the updated application, back up the database and run `docs/migrations/001-processed-match-user-key.sql` once. Fresh databases get the composite primary key through JPA. Hibernate `ddl-auto=update` alone must not be relied on to replace the old primary key. The migration preserves current rows and totals.

Old markers committed without credited minutes by the previous bug cannot be identified from the current schema: no per-run ledger records which markers contributed to totals. Audit/reconcile those historical totals separately; do not blindly delete all markers, since that would double-credit valid settlements. Pagination only recovers matches still exposed by Riot's API. Full-history scanning increases API calls and transaction duration; API failures roll back and leave the user retryable.

## Verification

`bash gradlew test --tests com.example.demo.service.RiotSettlementServiceTest --tests com.example.demo.service.LolCalculatorServiceTest`

Five H2/JPA regression tests cover rollback on a later detail failure, retry/idempotency, independent commits under a rolled-back Batch-like outer transaction, shared-game composite identity, more than 20 matches with an older gap and duplicate page IDs, later-page failures, and database write/commit failure. Four HTTP-client tests cover pagination query parameters, parsing, empty pages, error propagation and invalid page inputs. These tests use neither a real Riot key nor a MySQL server.

In the editing environment the Gradle distribution could not be downloaded (`Network is unreachable`). Tests were added but could not be executed; compilation and runtime results remain unverified. `git diff --check` passed. The existing full-suite `DemoApplicationTests` also requires the application's MySQL/Riot configuration and was not run.
