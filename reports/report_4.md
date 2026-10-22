# Метрики: VictoriaMetrics + экспорт метрик url-shortener


## 1. В течении выполнения

В minikube к приложению  добавлены:

- VictoriaMetrics single-node, установленная Helm-чартом;
- экспорт runtime/framework-метрик через Spring Boot Actuator + Micrometer Prometheus;
- пять бизнес-метрик доменной области;
- экспорт метрик PostgreSQL через `postgres-exporter` (sidecar в поде Postgres);
- сбор всех метрик в VictoriaMetrics через service discovery по аннотациям `prometheus.io/*`.

## 2. Стек

| Компонент | Технология |
|---|---|
| Приложение | Java 25, Spring Boot 4.1.1, Maven (multi-module) |
| Сервисы | `url-service` (CRUD + редиректы), `moderation-service` (проверка коротких кодов) |
| БД | PostgreSQL 16 |
| Миграции | Liquibase (отдельный Job в Kubernetes) |
| Метрики приложения | `spring-boot-starter-actuator`, `micrometer-registry-prometheus` |
| Метрики PostgreSQL | `prometheuscommunity/postgres-exporter:v0.15.0` |
| TSDB | VictoriaMetrics single-node, Helm-чарт `victoria-metrics-single` |
| Оркестрация | minikube, ingress-nginx |

## 3. Соответствие требованиям

| Требование | Реализация | Статус |
|---|---|---|
| Установить VictoriaMetrics Helm-чартом | релиз `vmsingle` (`victoria-metrics-single 0.49.0`, app `v1.154.0`) в namespace `url-shortener-namespace`, values — `k8s/metrics/guide-vmsingle-values.yaml` |
| Экспорт runtime/framework-метрик | Actuator + Micrometer Prometheus в обоих сервисах, отдельные management-порты 8082/8083 |
| Не менее 4 бизнес-метрик | 5 метрик: создание ссылок, редиректы, отклонения кастомных кодов, время генерации токена, проверки модерации | 
| Экспорт метрик PostgreSQL | sidecar `postgres-exporter` в поде `url-service-postgres-0`, порт 9187 |
| Service discovery через `prometheus.io/*` | аннотации на pod template деплойментов и StatefulSet + job `kubernetes-pods` в scrape-конфиге VictoriaMetrics | 

## 4. Развёртывание VictoriaMetrics

Использован официальный Helm-чарт:

```bash
helm repo add vm https://victoriametrics.github.io/helm-charts
helm repo update

helm upgrade --install vmsingle vm/victoria-metrics-single \
  -n url-shortener-namespace \
  -f k8s/metrics/guide-vmsingle-values.yaml
```

Файл `k8s/metrics/guide-vmsingle-values.yaml`:

- `server.scrape.enabled: true` — включает сбор метрик;
- `server.scrape.configMap: ""` — конфиг берётся из values, а не из внешнего ConfigMap;
- `global.scrape_interval: 15s`;
- job `kubernetes-pods` с `role: pod`, который читает аннотации `prometheus.io/scrape`, `prometheus.io/path`, `prometheus.io/port`.

Особенность версии чарта `0.49.0`: отдельный `vmagent` не разворачивается, скрейпинг выполняет сам процесс VictoriaMetrics (`vmsingle-victoria-metrics-single-server`). Поэтому проверка выполняется через сервис/под `vmsingle-victoria-metrics-single-server`, а не через `vm-single-victoria-metrics-agent` из примера задания.

Проверка:

```bash
helm list -n url-shortener-namespace
kubectl get pods -n url-shortener-namespace
kubectl get svc -n url-shortener-namespace

kubectl -n url-shortener-namespace port-forward \
  svc/vmsingle-victoria-metrics-single-server 8428:8428
```

VMUI: `http://localhost:8428/vmui/`
Статус таргетов: `http://localhost:8428/targets`

## 5. Runtime/framework-метрики

В `url-service/pom.xml` и `moderation-service/pom.xml` добавлены зависимости:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

В `application.yaml` обоих сервисов включён Prometheus-endpoint и вынесен отдельный management-порт:

```yaml
management:
  health:
    probes:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,prometheus
  server:
    port: ${URL_SERVICE_METRICS_PORT} # для moderation-service — MODERATION_SERVICE_METRICS_PORT
```

Порты:

| Сервис | HTTP приложения | Метрики |
|---|---|---|
| url-service | 80 | 8082, `/actuator/prometheus` |
| moderation-service | 8081 | 8083, `/actuator/prometheus` |

«Из коробки» экспортируются стандартные метрики:

- JVM: `jvm_memory_used_bytes`, `jvm_gc_pause_seconds_*`, `jvm_threads_live_threads`, `jvm_classes_loaded_classes` и др.;
- процесс: `process_cpu_usage`, `process_uptime_seconds`, `process_start_time_seconds`;
- HTTP: `http_server_requests_seconds_*`;
- HikariCP в url-service: `hikaricp_connections_*`, `hikaricp_connections_acquire_seconds_*`;
- Tomcat: `tomcat_sessions_*`;
- логирование: `logback_events_total`.

Проверка напрямую с пода:

```bash
kubectl -n url-shortener-namespace exec deploy/url-service-deployment -- \
  wget -qO- http://localhost:8082/actuator/prometheus | grep '^jvm_'

kubectl -n url-shortener-namespace exec deploy/moderation-service-deployment -- \
  wget -qO- http://localhost:8083/actuator/prometheus | grep '^jvm_'
```

## 6. Бизнес-метрики

Реализованы в `url-service/src/main/java/.../utils/UrlMetrics.java`, `.../utils/ShortTokenGenerator.java` и `moderation-service/.../ShortCodeCheckerImpl.java`.

| Метрика в коде | Имя в Prometheus | Тип | Лейблы | Где инкрементируется | Смысл |
|---|---|---|---|---|---|
| `url.created` | `url_total` | Counter | `type=random\|custom` | `UrlServiceImpl.createShortUrl` (стр. 65), `createCustomShortUrl` (стр. 109) | количество созданных коротких ссылок по способу создания |
| `url.redirects` | `url_redirects_total` | Counter | `result=found\|not_found` | `UrlServiceImpl.getLongUrl` (стр. 118) | количество редиректов, успешных и с несуществующим кодом |
| `url.custom.rejected` | `url_custom_rejected_total` | Counter | `reason=...` | `UrlServiceImpl.createCustomShortUrl` (стр. 85, 93) | отклонённые кастомные коды: занятый код / запрещённые слова |
| `url.token.generation.duration` | `url_token_generation_duration_seconds_*` | Timer (histogram) | `class`, `method`, `exception` | `ShortTokenGenerator.generateShortToken` (стр. 20–25, `@Timed`) | время генерации уникального короткого токена |
| `moderation.shortcode.checks` | `moderation_shortcode_checks_total` | Counter | `result=valid\|not_valid` | `ShortCodeCheckerImpl.isAllowed` (стр. 36) | количество проверок кодов модерацией |

Итого пять бизнес-метрик. Метрики создания и редиректов имеют по две серии за счёт лейблов; после первого отклонения появляется `url_custom_rejected_total`.

### Примечание по имени `url.created`

Micrometer/Prometheus считает суффикс `.created` зарезервированным для counter-метрик и срезает его при экспорте, поэтому в текущем образе метрика видна как `url_total{type="random|custom"}`, а не `url_created_total`. Семантика при этом корректна. Для более читаемого имени рекомендуется переименовать её, например, в `url.creation` (`url_creation_total`).

## 7. Экспорт метрик PostgreSQL

В StatefulSet `url-service-postgres` добавлен sidecar:

```yaml
- name: postgres-exporter
  image: prometheuscommunity/postgres-exporter:v0.15.0
  ports:
    - name: metrics
      containerPort: 9187
  env:
    - name: DATA_SOURCE_NAME
      value: "postgresql://$(POSTGRES_USER):$(POSTGRES_PASSWORD)@localhost:5432/$(POSTGRES_DB)?sslmode=disable"
```

Контейнеры пода имеют общее сетевое пространство, поэтому exporter подключается к Postgres через `localhost:5432`. Отдельный Service для exporter не требуется: pod discovery обращается к IP пода на порт 9187.

Экспортируемые метрики (примеры):

- `pg_up` — доступность БД;
- `pg_database_size_bytes` — размер БД;
- `pg_stat_database_numbackends` — число подключений;
- `pg_stat_activity_*`, `pg_settings_*` и другие стандартные метрики exporter.

Проверка:

```bash
kubectl -n url-shortener-namespace exec url-service-postgres-0 -c postgres-exporter -- \
  wget -qO- http://localhost:9187/metrics | grep -E '^(pg_up|pg_database_size_bytes)'
```

## 8. Service discovery по аннотациям

Аннотации добавлены на pod template (не на сам Deployment/StatefulSet), чтобы они попали на поды:

```yaml
template:
  metadata:
    annotations:
      prometheus.io/scrape: "true"
      prometheus.io/port: "8082"
      prometheus.io/path: "/actuator/prometheus"
```

Для сервисов и Postgres:

| Объект | `prometheus.io/port` | `prometheus.io/path` |
|---|---|---|
| `url-service-deployment` | 8082 | `/actuator/prometheus` |
| `moderation-service-deployment` | 8083 | `/actuator/prometheus` |
| `url-service-postgres` (StatefulSet) | 9187 | `/metrics` |

В scrape-конфиге VictoriaMetrics добавлен job:

```yaml
- job_name: "kubernetes-pods"
  kubernetes_sd_configs:
    - role: pod
  relabel_configs:
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      action: keep
      regex: "true"
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)
    - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
      action: replace
      regex: ([^:]+)(?::\d+)?;(\d+)
      replacement: $1:$2
      target_label: __address__
    - action: labelmap
      regex: __meta_kubernetes_pod_label_(.+)
    - source_labels: [__meta_kubernetes_namespace]
      target_label: namespace
    - source_labels: [__meta_kubernetes_pod_name]
      target_label: pod
```

