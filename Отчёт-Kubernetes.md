# Отчёт: запуск URL Shortener в Kubernetes

Проект — два микросервиса (`url-service`, `moderation-service`) + PostgreSQL + Liquibase-миграция.
Все манифесты лежат в `k8s/` и ставятся в изолированный неймспейс.

## 1. Какие абстракции Kubernetes использованы

Всего 9 YAML-файлов в `k8s/`:

| # | Абстракция | Имя | Файл | Назначение |
|---|---|---|---|---|
| 1 | `Namespace` | `url-shortener-namespace` | `k8s/namespaces/url-shortener-namespace.yaml` | Изоляция всего стека от остальных приложений кластера. Все остальные объекты созданы в этом неймспейсе. |
| 2 | `ConfigMap` | `url-service-config` | `k8s/configmaps/url-service-keymap.yaml` | Некритичная конфигурация `url-service`: `POSTGRES_HOST=url-postgres-service`, `POSTGRES_PORT=5432`, `POSTGRES_DB=url-service-db`, `URL_SERVICE_HOST=url-shortener.local`, `URL_SERVICE_PORT=80`, `MODERATION_SERVICE_HOST=moderation-service`, `MODERATION_SERVICE_PORT=8081`, `HTTP_READ_TIMEOUT=3000`, `HTTP_CONNECT_TIMEOUT=3000`. Маппится 1-в-1 на переменные из `url-service/src/main/resources/application.yaml` (`server.port`, `spring.datasource.url`, `moderation-client.base-url` и т.д.). |
| 3 | `ConfigMap` | `moderation-service-config` | `k8s/configmaps/moderaton-service-keymap.yaml` | Конфигурация `moderation-service`: `MODERATION_SERVICE_PORT=8081`. Маппится на `server.port` в `moderation-service/src/main/resources/application.yaml`. |
| 4 | `Secret` (Opaque) | `url-shortener-secrets` | `k8s/secrets/url-service-secrets.yaml` | Чувствительные данные: `POSTGRES_USER`, `POSTGRES_PASSWORD` (`stringData`). Не хранятся в ConfigMap и не светятся в коде. Потребляются через `secretKeyRef` / `secretRef`. |
| 5 | `Deployment` + `Service` (ClusterIP) | `url-service-deployment` / `url-service` | `k8s/deploymets/url-service-deployment.yaml` | Stateless-приложение. `replicas: 2`, образ `mkhalez/url-service:0.2`, `imagePullPolicy: IfNotPresent`, `containerPort: 80` (`url-http`). Конфиг через `envFrom: configMapRef(url-service-config) + secretRef(url-shortener-secrets)`. Пробы: `readinessProbe GET /actuator/health/readiness` (15/5с), `livenessProbe GET /actuator/health/liveness` (30/10с). Сервис `ClusterIP 80 -> targetPort url-http`, селектор `app: url-service`. |
| 6 | `Deployment` + `Service` (ClusterIP) | `moderation-service-deployment` / `moderation-service` | `k8s/deploymets/moderation-service-deployment.yaml` | Stateless-проверяльщик кодов. `replicas: 2`, образ `mkhalez/moderation-service:0.2`, `containerPort: 8081` (`moderation-http`). Конфиг через `envFrom: configMapRef(moderation-service-config)`. Пробы аналогичные (`/actuator/health/readiness`, `/actuator/health/liveness`). Сервис `ClusterIP 8081 -> targetPort moderation-http`, селектор `app: moderation-service`. |
| 7 | `StatefulSet` + headless `Service` | `url-service-postgres` / `url-postgres-service` | `k8s/statefulsets/url-service-postgres-statefulset.yaml` | Состояние (PostgreSQL 16). `replicas: 1`, `serviceName: url-postgres-service`. Headless-сервис `clusterIP: None`, `port 5432 -> pg`, селектор `app: url-postgres` — даёт стабильное DNS-имя `url-postgres-service` для подключений. Переменные: `POSTGRES_DB` из ConfigMap, `POSTGRES_USER/PASSWORD` из Secret, `PGDATA=/var/lib/postgresql/data/pgdata`. `volumeMounts: pgdata -> /var/lib/postgresql/data`, `volumeClaimTemplates: 1Gi, ReadWriteOnce` — переживает пересоздание пода. Пробы `exec: pg_isready -U $POSTGRES_USER -d $POSTGRES_DB` (readiness 5/5с, liveness 30/10с). |
| 8 | `Job` | `url-service-postgres-migration` | `k8s/jobs/url-posgres-migration.yaml` | One-shot накат схемы `backoffLimit: 3`, `activeDeadlineSeconds: 600`, `restartPolicy: Never`. `initContainer wait-for-db` (образ `postgres:16`): `until pg_isready -h $POSTGRES_HOST -p $POSTGRES_PORT -U $POSTGRES_USER; do sleep 3; done` — ждёт готовности БД. Основной контейнер `liquibase`, образ `mkhalez/url-migration:0.2`, `args: ["update"]`, env: `POSTGRES_DB/HOST/PORT` из ConfigMap, `LIQUIBASE_COMMAND_URL=jdbc:postgresql://$(POSTGRES_HOST):$(POSTGRES_PORT)/$(POSTGRES_DB)`, `LIQUIBASE_COMMAND_USERNAME/PASSWORD` из Secret, `LIQUIBASE_COMMAND_CHANGELOG_FILE=changelog/changelog.xml`. |
| 9 | `Ingress` | `url-shortener-ingress` | `k8s/ingreses/url-shortener-ingress.yaml` | Внешняя точка входа. `ingressClassName: nginx`, правило `host: url-shortener.local`, `path: / pathType: Prefix -> service url-service:80`. Только `url-service` торчит наружу; БД и модерация недоступны извне. |

Порядок применения, вытекающий из зависимостей:

```text
Namespace -> ConfigMaps + Secret -> StatefulSet (Postgres) + headless Service
  -> Job (ждёт Postgres, накатывает Liquibase)
  -> Deployments + ClusterIP Services (url-service, moderation-service)
  -> Ingress
```

## 2. Как сервисы приложения взаимодействуют между собой

```text
                   +----------------------+
                   |  Client (браузер)    |
                   |  Host: url-shortener |
                   |       .local         |
                   +----------+-----------+
                              | HTTP :80, path /
                              v
                   +----------------------+
                   | Ingress nginx        |
                   | url-shortener-ingress|
                   +----------+-----------+
                              | -> url-service:80
                              v
  +--------------------------------------------------+
  | url-service (Deployment, 2 реплики)              |
  | ClusterIP url-service:80 -> pod:80 (url-http)    |
  | POST /urls, POST /urls/custom, GET /urls/{id},   |
  | GET /{shortCode} -> 302                          |
  +--+------------------------------+----------------+
     |                              |
     | JDBC                         | HTTP POST /shortcode
     | jdbc:postgresql://           | {"shortCode":"..."}
     | url-postgres-service:5432/   | -> {"shortCode":"...","status":"VALID|NOT_VALID"}
     | url-service-db               | Host: moderation-service (.env: MODERATION_SERVICE_HOST/PORT)
     v                              v
  +------------------+   +-----------------------------+
  | Postgres         |   | moderation-service          |
  | StatefulSet (1)  |   | (Deployment, 2 реплики)     |
  | headless Service |   | ClusterIP                   |
  | url-postgres-    |   | moderation-service:8081     |
  | service:5432     |   | -> pod:8081, stateless, БД  |
  +------------------+   +-----------------------------+
           ^
           | pg_isready + Liquibase update
           |
  +------------------+
  | Job              |
  | url-service-     |
  | postgres-        |
  | migration        |
  +------------------+
```

### 2.1. Внешний трафик: Client -> Ingress -> url-service

- Единственный публичный путь — `Ingress url-shortener-ingress` (`nginx`, `url-shortener.local`, `/ Prefix`).
- Ingress проксирует на `Service url-service:80` (`ClusterIP`), тот балансирует на 2 пода `url-service-deployment` (`targetPort: url-http = 80`).
- `moderation-service` и `url-postgres-service` типа `ClusterIP`/`Headless` без Ingress-правил — извне недоступны.

### 2.2. url-service -> PostgreSQL (JDBC, stateful)

- Строка подключения собирается из env: `spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}` (`application.yaml` url-service).
- В Kubernetes значения приходят из `url-service-config` (`POSTGRES_HOST=url-postgres-service`, `POSTGRES_PORT=5432`, `POSTGRES_DB=url-service-db`) и `url-shortener-secrets` (`POSTGRES_USER/PASSWORD`).
- `url-postgres-service` — headless (`clusterIP: None`), резолвится в IP пода `url-service-postgres-0`.
- Схема не создаётся приложением (`ddl-auto: validate`), а накатывается Job'ом до старта (см. 2.4).

### 2.3. url-service -> moderation-service (HTTP/REST, синхронно)

- Проверка каждого нового кода перед сохранением: `UrlService` -> `ModerationGateway.check()` -> `ModerationServiceClient.isAllowed()` (`RestClient` + `HttpServiceProxyFactory` из `ClientConfig.java`, таймауты 3000 мс из ConfigMap).
- `base-url` собирается как `http://${MODERATION_SERVICE_HOST}:${MODERATION_SERVICE_PORT}` = `http://moderation-service:8081` (значения из `url-service-config`).
- Контракт: `POST http://moderation-service:8081/shortcode`, body `{"shortCode":"..."}`, ответ `{"shortCode":"...","status":"VALID|NOT_VALID"}`. При `NOT_VALID`, таймауте или 5xx создание ссылки отклоняется (`ModerationUnavailableException` / `ModerationIntegrationException`). Других связей между сервисами нет — это единственная межсервисная зависимость.
- `moderation-service` stateless, БД не имеет, список запрещённых слов (`spamm, slopp, booot`) — в памяти из `application.yaml`. Поэтому его можно держать в 2 репликах за `ClusterIP`-сервисом без sticky-сессий.

### 2.4. Job миграции -> PostgreSQL (init + Liquibase)

- `initContainer wait-for-db` блокирует старт Liquibase, пока `pg_isready -h url-postgres-service -p 5432` не вернёт успех.
- Основной контейнер строит `LIQUIBASE_COMMAND_URL=jdbc:postgresql://url-postgres-service:5432/url-service-db` из ConfigMap-переменных и выполняет `update` по `changelog/changelog.xml`. `backoffLimit: 3` даёт 3 ретрая при падении.

### 2.5. Пробы и обнаружение сервисов (Service Discovery)

- `readinessProbe` убирает неготовый под из балансировки Service (`/actuator/health/readiness` у Java-сервисов, `pg_isready` у Postgres), `livenessProbe` перезапускает зависший под (`/actuator/health/liveness`, `pg_isready`).
