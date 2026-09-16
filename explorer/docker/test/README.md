# Docker environment for UI tests

The normal `docker-compose.yml` continues to use Google APIs. This separate Compose project uses its own images, network, database and test credentials.

Run commands from the `explorer/` directory containing `pom.xml`.

## Start the test environment

```powershell
docker compose -f docker-compose.test.yml up -d --build
```

The first build downloads dependencies and can take several minutes. The frontend source defaults to `D:/4th course/diploma/frontend`. On another machine, set its location before building:

```powershell
$env:TEST_FRONTEND_PATH = 'D:/path/to/frontend'
```

| Component | Test address |
| --- | --- |
| Frontend | http://localhost:3001 |
| Backend | http://localhost:8081 |
| PostgreSQL | localhost:5434, database `explorer_test` |
| WireMock | http://localhost:8089, started by JUnit |

The test database is seeded with Ukraine, the museum category and the `Test123` user from `ExistingUser`. It does not copy data from the normal database. Seed operations are safe to repeat. Browser Google Maps is disabled only in the test frontend build.

## Run tests

Start `TripTest` in the IDE, or run:

```powershell
mvn "-Dtest=TripTest" "-Dbrowser.headless=true" test
```

JUnit starts and stops WireMock on port 8089. It never starts or stops Docker. The trip test fills the form, creates the trip, checks the details heading, and verifies that Places and Geocoding requests reached WireMock. Do not run multiple WireMock test classes concurrently on that fixed port.

`test.properties` defaults to the test frontend. Login and registration pages also use `TestConfig`. Omit `-Dbrowser.headless=true` to see the browser. The test frontend can sign in without WireMock, but creating trips requires the running mock and its stubs. Tests create records only in the test database.

The backend uses HTTP/1.1 for the test environment to avoid the JDK HTTP/2 cleartext upgrade issue with WireMock. Its Google key is a dummy test value; local application secrets are excluded from the test backend image.

## Inspect or stop

```powershell
docker compose -f docker-compose.test.yml ps
docker compose -f docker-compose.test.yml logs --tail=100 backend
docker compose -f docker-compose.test.yml down
```

Stopping this project preserves its database volume and does not stop the normal website. Rebuild with `up -d --build` after changing backend or frontend source. JSON stubs are loaded by tests and do not require rebuilding Docker.
