# Docker Compose

Run from `D:/explorer/explorer` with Docker Desktop (Linux containers) and Compose 2.17+.
Keep your existing `.env` credentials: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
and `GOOGLE_API_KEY`. Set `DB_NAME` if the database name is not `explorer`.
Compose overrides the database host with `db`; your local `DB_URL` is unchanged.
Set `VITE_GOOGLE_MAPS_API_KEY` for browser maps. This key is included in the frontend bundle.

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f backend recommendation-service
```

All application images build from source; a separate Maven build is unnecessary.
Frontend source defaults to `D:/4th course/diploma/frontend` and recommender source
to `../travel-recommender-service`. Override `FRONTEND_PATH` or `RECOMMENDER_PATH`
in `.env` if needed. The frontend proxies `/api` to the backend through Nginx.
The recommender starts after the backend is healthy and its database tables exist.

| Service | Local address |
| --- | --- |
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Recommender | http://localhost:8001/health |
| PostgreSQL | localhost:5433 |

Ports can be overridden with the variables listed in `.env.example`.
To use an environment file from another directory:

```powershell
docker compose --env-file 'D:/path/to/.env' up -d --build
```

PostgreSQL 16 data persists in the named volume `explorer_pgdata`.
If the earlier `explorer-postgres` container uses this volume, stop it before starting Compose:

```powershell
docker stop explorer-postgres
```

An existing volume requires its existing credentials and compatible PostgreSQL 16 data.
Changing initialization variables does not change existing database credentials.
Override `POSTGRES_VOLUME_NAME` to select a different volume.

Stop the stack while keeping its data:

```powershell
docker compose down
```

Do not add `-v` if you want to keep the database volume.
