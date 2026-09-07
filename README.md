# Travel Explorer

Travel Explorer — вебзастосунок для пошуку та планування подорожей. Він допомагає знаходити цікаві туристичні місця, зберігати їх та створювати власні подорожі.

## Про проєкт

Користувач може переглядати туристичні місця, ознайомлюватися з інформацією про них і зберігати цікаві варіанти. Збережені місця можна використовувати під час планування подорожі та організації активностей за днями.

На основі вибраних інтересів та оцінок користувачів система формує персоналізовані рекомендації, допомагаючи знаходити місця для наступних подорожей.

Цей репозиторій містить REST API на Spring Boot і Python-сервіс рекомендацій, підключений як Git submodule. Фронтенд зберігається окремо.

## Мета проєкту

Мета Travel Explorer — спростити пошук туристичних місць та планування подорожей, об’єднавши пошук, персоналізовані рекомендації та організацію подорожей в одному застосунку.

## Основні можливості

- Реєстрація, вхід і вихід користувачів, автентифікація через JWT у cookie.
- Створення та редагування подорожей із розкладом активностей за днями.
- Пошук місць через Google Places, отримання фотографій і геокодування.
- Персоналізовані рекомендації: оцінка відповідності інтересам та collaborative filtering на основі SVD. Типові ваги — 0.6 і 0.4 відповідно.
- Збереження цікавих місць, додавання та заміна активностей, зміна їхнього порядку.
- Оцінювання подорожей і активностей, перегляд каталогу подорожей.
- Експорт подорожі у PDF.

## Технології

| Компонент | Технології |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.6, Spring Web, Spring Data JPA |
| Автентифікація | Spring Security, JJWT, BCrypt |
| База даних | PostgreSQL 17 |
| Рекомендації | Python 3.11, FastAPI, SVD |
| Зовнішні дані | Google Places API, Google Geocoding API |
| PDF | OpenPDF |
| Тести | JUnit 5, Playwright |
| Запуск | Maven, Docker Compose |

Java-код компілюється для Java 17; Docker-образ backend використовує JDK 21.

## Структура репозиторію

```text
TravelExplorer/
├── explorer/
│   ├── src/main/java/com/travel/explorer/
│   │   ├── controller/   # REST API
│   │   ├── entities/     # JPA-сутності
│   │   ├── repo/         # Репозиторії даних
│   │   ├── service/      # Планування та рекомендації
│   │   ├── google/       # Інтеграція з Google API
│   │   ├── security/     # Автентифікація та JWT
│   │   └── payload/      # DTO запитів і відповідей
│   ├── src/main/resources/application.properties
│   ├── src/test/         # Браузерні тести
│   ├── pom.xml
│   ├── Dockerfile
│   └── docker-compose.yml
└── travel-recommender-service/  # Git submodule
    ├── Src/
    └── Dockerfile
```

## Підготовка

Потрібні Git, JDK 17 або новіший, Maven 3.9+ та Docker із Compose. Для запуску рекомендацій без Docker потрібен Python 3.11.

Клонуйте репозиторій разом із сервісом рекомендацій:

```sh
git clone --recurse-submodules https://github.com/jlmp3mp44/TravelExplorer.git
cd TravelExplorer
```

Для вже клонованого репозиторію:

```sh
git submodule update --init --recursive
```

Команди нижче використовують встановлений `mvn`: скрипти Maven Wrapper є в репозиторії, але каталог `.mvn` не відстежується Git.

## Конфігурація

Backend потребує таких параметрів. Реальні ключі задавайте локально або через змінні середовища.

| Spring property | Змінна середовища | Значення для локальної розробки |
| --- | --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/explorer` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `admin` для наявного Compose |
| `spring.app.jwtSecret` | `SPRING_APP_JWTSECRET` | Випадковий Base64-ключ, щонайменше 32 байти до кодування |
| `spring.app.jwtCookieName` | `SPRING_APP_JWTCOOKIENAME` | Наприклад, `explorer-jwt` |
| `google.api.key` | `GOOGLE_API_KEY` | Власний ключ Google Places і Geocoding |
| `recommendation.service.url` | `RECOMMENDATION_SERVICE_URL` | `http://localhost:8001` |

За замовчуванням `application.properties` використовує Docker-адреси `db:5432` і `recommendation-service:8001`. Для запуску backend на хості їх потрібно перевизначити, як у таблиці.

Профіль `secrets` підключений у конфігурації. Альтернатива змінним середовища — файл `explorer/src/main/resources/application-secrets.properties`, який ігнорується Git. Такий файл потрапляє у JAR під час збірки, тому для образів, які поширюються, передавайте секрети під час запуску.

## Запуск через Docker Compose

Виконуйте команди з каталогу `explorer/`:

```sh
cd explorer
mvn clean package -DskipTests
docker build -t explorerapi .
docker build -t recommendation-service ../travel-recommender-service
```

Compose посилається на готові образи backend і сервісу рекомендацій, тому їх потрібно зібрати перед запуском. Тести пропускаються на цьому кроці, оскільки потребують запущеного застосунку та браузера.

Створіть локальний файл `compose.local.yml` у цьому ж каталозі:

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

Задайте `SPRING_APP_JWTSECRET` та `GOOGLE_API_KEY` у середовищі поточного термінала. Наприклад, у PowerShell:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:SPRING_APP_JWTSECRET = [Convert]::ToBase64String($jwtBytes)
$env:GOOGLE_API_KEY = '<your-google-api-key>'
```

Запустіть базу, backend і рекомендації:

```sh
docker compose -f docker-compose.yml -f compose.local.yml up -d db backend recommendation-service
docker compose -f docker-compose.yml -f compose.local.yml logs -f backend
```

| Сервіс | Адреса з хоста |
| --- | --- |
| Backend | `http://localhost:8080` |
| Сервіс рекомендацій | `http://localhost:8001` |
| PostgreSQL | `localhost:5433`, база `explorer` |
| Фронтенд, якщо запущено | `http://localhost:3000` |

У `docker-compose.yml` шлях `frontend.build.context` зараз заданий як `D:/4th course/diploma/frontend`. Для запуску UI замініть його на шлях до свого фронтенду або перевизначте в `compose.local.yml`. За потреби задайте `VITE_GOOGLE_MAPS_API_KEY`, після чого запустіть:

```sh
docker compose -f docker-compose.yml -f compose.local.yml up -d --build frontend
```

Compose очікує старт контейнера БД, але не перевіряє її готовність. Якщо backend завершився при першому запуску через недоступність PostgreSQL, дочекайтеся готовності БД і повторіть запуск backend.

## Backend без Docker

Із каталогу `explorer/` запустіть лише PostgreSQL:

```sh
docker compose up -d db
```

У PowerShell налаштуйте середовище, додатково задавши JWT-ключ і Google API key з попереднього розділу:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/explorer'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = 'admin'
$env:SPRING_APP_JWTCOOKIENAME = 'explorer-jwt'
$env:RECOMMENDATION_SERVICE_URL = 'http://localhost:8001'
mvn spring-boot:run
```

Для рекомендацій можна використати Docker-образ або окремий Python-процес. У новому терміналі, з кореня репозиторію:

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

Сервіс рекомендацій підтримує періодичне перенавчання на оцінках із PostgreSQL. Деталі параметрів — у [README сервісу рекомендацій](travel-recommender-service/README.md).

## Основні маршрути API

Базова адреса: `http://localhost:8080`. Повні DTO та параметри запитів описані кодом у `payload/` і `controller/`.

| Метод | Шлях | Призначення |
| --- | --- | --- |
| POST | `/api/auth/signup` | Реєстрація |
| POST | `/api/auth/signin` | Вхід та встановлення JWT-cookie |
| POST | `/api/auth/signout` | Вихід |
| GET / PUT | `/api/user` | Перегляд та зміна профілю |
| GET | `/api/public/countries` | Країни |
| GET | `/api/public/countries/{countryId}/cities` | Міста країни |
| GET | `/api/public/place-categories` | Категорії місць |
| POST | `/api/public/places/search-text` | Пошук місць |
| GET / POST | `/api/public/trips` | Каталог та створення подорожі |
| GET / PUT / DELETE | `/api/public/trips/{tripId}` | Перегляд, зміна та видалення подорожі |
| GET | `/api/public/trips/{tripId}/pdf` | PDF подорожі |
| PUT | `/api/public/trips/{tripId}/days/{dayId}/activities/order` | Порядок активностей |
| POST | `/api/public/trips/{tripId}/activities/{activityId}/replace-smart` | Автоматична заміна активності |
| POST | `/api/public/trips/{tripId}/ratings` | Оцінка подорожі |
| GET / POST | `/api/user/interesting-places` | Збережені цікаві місця |

Для запитів після входу клієнт має зберігати та передавати cookie; у браузерному `fetch` використовуйте `credentials: 'include'`. CORS дозволяє `http://localhost:5173` і `http://localhost:3000`. Префікс `/api/public` не гарантує анонімного доступу: зокрема, каталог подорожей потребує входу.

## Тести

Браузерні тести розташовані в `explorer/src/test/java/com/travel/explorer/ui`. Вони перевіряють сценарії входу та реєстрації; сценарій створення подорожі ще розробляється й містить `page.pause()`.

Перед запуском підніміть backend, БД і фронтенд. Перевірте `base.url` у `explorer/src/test/resources/test.properties` (за замовчуванням `http://localhost:3000`) та наявність користувача, заданого в `ExistingUser.java`.

З каталогу `explorer/` встановіть Chromium для Playwright і запустіть тести входу та реєстрації:

```sh
mvn exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
mvn test "-Dtest=LoginTest,RegisterTest"
```

У поточній конфігурації браузер відкривається у видимому режимі (`headless=false`), тому потрібне графічне середовище. Команда `mvn test` також включає незавершений тест подорожі з інтерактивною паузою.

## Особливості поточної версії

- Hibernate оновлює схему через `spring.jpa.hibernate.ddl-auto=update`.
- Під час старту `SecurityConfig` створює демонстраційні облікові записи `user1` та `admin`, якщо їх немає. Перед публічним розгортанням змініть цю ініціалізацію та налаштування доступу.
- Каталог країн, міст та інших довідникових даних залежить від наповнення БД; дамп БД не входить до репозиторію.
- Дані PostgreSQL у Compose зберігаються в томі `postgres_data`.
