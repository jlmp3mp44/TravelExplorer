# Travel Explorer

Travel Explorer is a web application for discovering places and planning trips. It helps users find interesting destinations, save their favorites, and create their own trips.

## About the Project

Users can browse tourist attractions, view information about them, and save places they would like to visit. Saved places can be used when planning trips and organizing daily activities.

The system provides personalized recommendations based on selected interests and user ratings, helping users discover places for future trips.

This repository contains a Spring Boot REST API and a Python recommendation service included as a Git submodule. The frontend is maintained separately.

## Project Goal

Travel Explorer aims to simplify discovering places and planning trips by bringing search, personalized recommendations, and trip organization into a single application.

## Key Features

- User registration, sign-in, and sign-out with JWT authentication using cookies.
- Trip creation and editing with daily activity schedules.
- Place search through Google Places, photo retrieval, and geocoding.
- Personalized recommendations combining interest matching and SVD-based collaborative filtering, with default weights of 0.6 and 0.4 respectively.
- Saved places, activity additions and replacements, and activity reordering.
- Trip and activity ratings, and a browsable trip catalog.
- Trip export to PDF.

## Technology Stack

| Component | Technologies |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.6, Spring Web, Spring Data JPA |
| Authentication | Spring Security, JJWT, BCrypt |
| Database | PostgreSQL 17 |
| Recommendations | Python 3.11, FastAPI, SVD |
| External data | Google Places API, Google Geocoding API |
| PDF | OpenPDF |
| Testing | JUnit 5, Playwright |
| Build and runtime | Maven, Docker Compose |

The Java source targets Java 17; the backend Docker image uses JDK 21.

## Repository Structure

```text
TravelExplorer/
├── explorer/
│   ├── src/main/java/com/travel/explorer/
│   │   ├── controller/   # REST API
│   │   ├── entities/     # JPA entities
│   │   ├── repo/         # Data repositories
│   │   ├── service/      # Planning and recommendations
│   │   ├── google/       # Google API integration
│   │   ├── security/     # Authentication and JWT
│   │   └── payload/      # Request and response DTOs
│   ├── src/main/resources/application.properties
│   ├── src/test/         # Browser tests
│   ├── pom.xml
│   ├── Dockerfile
│   └── docker-compose.yml
└── travel-recommender-service/  # Git submodule
    ├── Src/
    └── Dockerfile
```

## Prerequisites

Install Git, JDK 17 or later, Maven 3.9+, and Docker with Compose. Python 3.11 is required to run the recommendation service outside Docker.

Clone the repository with the recommendation service:

```sh
git clone --recurse-submodules https://github.com/jlmp3mp44/TravelExplorer.git
cd TravelExplorer
```

For an existing clone:

```sh
git submodule update --init --recursive
```

The commands below use an installed `mvn` executable. Maven Wrapper scripts are included, but the `.mvn` directory is not tracked in Git.

## Configuration

The backend requires the following settings. Supply actual secrets locally or through environment variables.

| Spring property | Environment variable | Local development value |
| --- | --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/explorer` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `admin` for the included Compose configuration |
| `spring.app.jwtSecret` | `SPRING_APP_JWTSECRET` | A random Base64-encoded key containing at least 32 bytes before encoding |
| `spring.app.jwtCookieName` | `SPRING_APP_JWTCOOKIENAME` | For example, `explorer-jwt` |
| `google.api.key` | `GOOGLE_API_KEY` | Your Google Places and Geocoding API key |
| `recommendation.service.url` | `RECOMMENDATION_SERVICE_URL` | `http://localhost:8001` |

By default, `application.properties` uses the Docker addresses `db:5432` and `recommendation-service:8001`. Override them as shown in the table when running the backend on the host.

The configuration includes the `secrets` profile. As an alternative to environment variables, use `explorer/src/main/resources/application-secrets.properties`, which is ignored by Git. This file is bundled into the JAR during the build, so inject secrets at runtime for images you distribute.

## Running with Docker Compose

From the repository root, enter `explorer/` and build the images:

```sh
cd explorer
mvn clean package -DskipTests
docker build -t explorerapi .
docker build -t recommendation-service ../travel-recommender-service
```

Compose references prebuilt backend and recommendation service images, so build them before starting the services. Tests are skipped at this stage because they require a running application and browser.

Create a local `compose.local.yml` file in the same directory:

```yaml
services:
  backend:
    environment:
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: admin
      SPRING_APP_JWTSECRET: ${SPRING_APP_JWTSECRET:?Set SPRING_APP_JWTSECRET}
      SPRING_APP_JWTCOOKIENAME: explorer-jwt
      GOOGLE_API_KEY: ${GOOGLE_API_KEY:?Set GOOGLE_API_KEY}
```

Set `SPRING_APP_JWTSECRET` and `GOOGLE_API_KEY` in your current terminal environment. For example, in PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:SPRING_APP_JWTSECRET = [Convert]::ToBase64String($jwtBytes)
$env:GOOGLE_API_KEY = '<your-google-api-key>'
```

Start the database, backend, and recommendation service:

```sh
docker compose -f docker-compose.yml -f compose.local.yml up -d db backend recommendation-service
docker compose -f docker-compose.yml -f compose.local.yml logs -f backend
```

| Service | Address on the host |
| --- | --- |
| Backend | `http://localhost:8080` |
| Recommendation service | `http://localhost:8001` |
| PostgreSQL | `localhost:5433`, database `explorer` |
| Frontend, if started | `http://localhost:3000` |

In `docker-compose.yml`, `frontend.build.context` is currently set to `D:/4th course/diploma/frontend`. To run the UI, replace it with the path to your frontend or override it in `compose.local.yml`. Set `VITE_GOOGLE_MAPS_API_KEY` if needed, then run:

```sh
docker compose -f docker-compose.yml -f compose.local.yml up -d --build frontend
```

Compose waits for the database container to start, but does not check database readiness. If the backend exits on the first run because PostgreSQL is unavailable, wait until the database is ready and start the backend again.

## Running the Backend Outside Docker

From `explorer/`, start PostgreSQL only:

```sh
docker compose up -d db
```

Configure the environment in PowerShell, also setting the JWT secret and Google API key from the previous section:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/explorer'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = 'admin'
$env:SPRING_APP_JWTCOOKIENAME = 'explorer-jwt'
$env:RECOMMENDATION_SERVICE_URL = 'http://localhost:8001'
mvn spring-boot:run
```

Run recommendations using the Docker image or a separate Python process. In a new terminal, starting from the repository root:

```powershell
cd travel-recommender-service/Src
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
$env:RECOMMENDER_DB_HOST = 'localhost'
$env:RECOMMENDER_DB_PORT = '5433'
$env:RECOMMENDER_DB_NAME = 'explorer'
$env:RECOMMENDER_DB_USER = 'postgres'
$env:RECOMMENDER_DB_PASSWORD = 'admin'
python -m uvicorn main:app --host 0.0.0.0 --port 8001
```

The recommendation service supports scheduled retraining using ratings from PostgreSQL. See the [recommendation service README](travel-recommender-service/README.md) for configuration details.

## Main API Endpoints

Base URL: `http://localhost:8080`. See the source code in `payload/` and `controller/` for complete DTOs and request parameters.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/signup` | Register a user |
| POST | `/api/auth/signin` | Sign in and set the JWT cookie |
| POST | `/api/auth/signout` | Sign out |
| GET / PUT | `/api/user` | View and update the profile |
| GET | `/api/public/countries` | List countries |
| GET | `/api/public/countries/{countryId}/cities` | List cities in a country |
| GET | `/api/public/place-categories` | List place categories |
| POST | `/api/public/places/search-text` | Search for places |
| GET / POST | `/api/public/trips` | Browse the catalog and create a trip |
| GET / PUT / DELETE | `/api/public/trips/{tripId}` | View, update, and delete a trip |
| GET | `/api/public/trips/{tripId}/pdf` | Export a trip to PDF |
| PUT | `/api/public/trips/{tripId}/days/{dayId}/activities/order` | Reorder activities |
| POST | `/api/public/trips/{tripId}/activities/{activityId}/replace-smart` | Automatically replace an activity |
| POST | `/api/public/trips/{tripId}/ratings` | Rate a trip |
| GET / POST | `/api/user/interesting-places` | View and save interesting places |

After signing in, clients must retain and send cookies. For browser `fetch` requests, use `credentials: 'include'`. CORS allows `http://localhost:5173` and `http://localhost:3000`. The `/api/public` prefix does not guarantee anonymous access; the trip catalog requires sign-in.

## Tests

Browser tests are located in `explorer/src/test/java/com/travel/explorer/ui`. They cover sign-in and registration scenarios. The trip creation scenario is still in development and contains `page.pause()`.

Before running tests, start the backend, database, and frontend. Check `base.url` in `explorer/src/test/resources/test.properties` (default: `http://localhost:3000`) and ensure the user configured in `ExistingUser.java` exists.

From `explorer/`, install Chromium for Playwright and run the sign-in and registration tests:

```sh
mvn exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
mvn test "-Dtest=LoginTest,RegisterTest"
```

The current configuration opens a visible browser (`headless=false`), so a graphical environment is required. Running `mvn test` also includes the unfinished trip test with its interactive pause.

## Current Implementation Notes

- Hibernate updates the schema through `spring.jpa.hibernate.ddl-auto=update`.
- At startup, `SecurityConfig` creates the demo accounts `user1` and `admin` if they do not exist. Update this initialization and the access settings before a public deployment.
- The availability of countries, cities, and other reference data depends on the database contents. A database dump is not included in the repository.
- PostgreSQL data in Compose is stored in the `postgres_data` volume.
