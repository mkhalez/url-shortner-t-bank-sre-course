# URL Shortener

Сервис укорачивания ссылок на Spring Boot (multi-module Maven проект, Java 25, Spring Boot 4.1.1).

Два микросервиса + PostgreSQL + Liquibase-миграции:

| Компонент | Описание |
|---|---|
| `url-service` | Основной CRUD для ссылок: создание случайного / кастомного короткого кода, получение по `id`, обновление, удаление, листинг и редирект `GET /{shortCode} → 302` на оригинальный URL. Перед сохранением кода ходит в `moderation-service` для проверки. Хранит данные в PostgreSQL (`ddl-auto: validate`, схема накатывается Liquibase). Длина автогенерации — 6 символов (`url.short-url-length`). |
| `moderation-service` | Проверка короткого кода на запрещённые слова. `POST /shortcode` с `{ "shortCode": "..." }` возвращает `{ "shortCode": "...", "status": "VALID" \| "NOT_VALID" }`. Проверка — case-insensitive `contains` по списку `app.moderation.banned-words` (по умолчанию `spamm, slopp, booot`). Stateless, без БД. |
| `url-postgres` | PostgreSQL 16. Таблица `url(id, short_code, long_url, created_at, last_modified_at)` + индекс на `short_code`. |
| `url-liquibase` | One-shot контейнер `liquibase/liquibase:4.29`, накатывает `url-service/migrations/changelog/changelog.xml` (`create-url-table.sql`, `create-url-index.sql`). |

Структура репозитория:

```
.
├── docker-compose.yaml
├── .env                     
├── pom.xml                     
├── url-service/
│   ├── Dockerfile
│   ├── migrations/        
│   └── src/main/...
├── moderation-service/
│   ├── Dockerfile
│   └── src/main/...
```

### API

`url-service` (по умолчанию `http://localhost:8080`):

- `POST /urls` — создать случайный код. Body: `{ "longUrl": "https://..." }` → `201 + { "id", "longUrl", "shortUrl" }`
- `POST /urls/custom` — создать кастомный код. Body: `{ "longUrl": "...", "customShortCode": "..." }`, код должен match `[a-zA-Z0-9_-]{5,10}` → `201`
- `GET /urls/{id}` — получить по id → `200`
- `GET /urls` — список всех → `200`
- `PUT /urls/{id}` — обновить `longUrl`. Body: `{ "longUrl": "..." }` → `200`
- `DELETE /urls/{id}` — удалить → `204`
- `GET /{shortCode}` — редирект, `shortCode` match `[a-zA-Z0-9_-]{5,10}` → `302 Found` с `Location: <longUrl>`

`moderation-service` (внутри compose доступен как `http://moderation-service:8081`):

- `POST /shortcode` — Body: `{ "shortCode": "..." }` → `200 + { "shortCode": "...", "status": "VALID" | "NOT_VALID" }`

## Необходимые env

Все переменные задаются через `.env` в корне. Шаблон — `.env.example`:

```bash
cp .env.example .env
```

Файл `.env` не коммитится и не включается в архив (см. `.gitignore`).
Перед запуском замените плейсхолдеры `change_me_*` на свои значения.

- POSTGRES_DB
- POSTGRES_USER
- POSTGRES_PASSWORD
- POSTGRES_PORT
- POSTGRES_HOST
- URL_SERVICE_PORT
- URL_SERVICE_HOST
- MODERATION_SERVICE_PORT
- MODERATION_SERVICE_HOST

## Быстрый запуск с помощью Docker Compose

```bash
cp .env.example .env
docker compose up -d
```

## Запуск в Kubernetes (minikube)

Секреты не хранятся в репозитории. Шаблон — `k8s/secrets/url-service-secrets.example.yaml`.
Перед первым `kubectl apply` создайте реальный файл из примера и подставьте свои значения
вместо плейсхолдеров `change_me_*`:

```bash
cp k8s/secrets/url-service-secrets.example.yaml k8s/secrets/url-service-secrets.yaml
```

Файл `k8s/secrets/url-service-secrets.yaml` не коммитится (см. `.gitignore`).

Порядок применения манифестов:

```bash
kubectl apply -f k8s/namespaces/url-shortener-namespace.yaml
kubectl apply -f k8s/configmaps/url-service-keymap.yaml \
  -f k8s/configmaps/moderaton-service-keymap.yaml \
  -f k8s/secrets/url-service-secrets.yaml
kubectl apply -f k8s/statefulsets/url-service-postgres-statefulset.yaml
kubectl apply -f k8s/jobs/url-posgres-migration.yaml
kubectl apply -f k8s/deploymets/url-service-deployment.yaml \
  -f k8s/deploymets/moderation-service-deployment.yaml
kubectl apply -f k8s/ingreses/url-shortener-ingress.yaml
```

Используемые образы:

- `postgres:16`
- `liquibase/liquibase:4.29`
- `mkhalez/url-service:latest`
- `mkhalez/moderation-service:latest`
