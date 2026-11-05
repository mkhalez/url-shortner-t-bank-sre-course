# Алертинг url-shortener

Набор манифестов для алертинга сервиса `url-service`:

```
Spring Actuator /actuator/prometheus
        ↓ scrape
VictoriaMetrics (vmsingle)
        ↓ PromQL
vmalert
        ↓
Alertmanager
        ↓
Telegram
```

## Файлы

| Файл | Назначение |
|---|---|
| `alerts-configmap.yaml` | Правила vmalert (4 алерта) |
| `vmalert-configmap.yaml` | Флаги vmalert в виде переменных окружения |
| `vmalert-deployment.yaml` | Deployment vmalert с requests/limits |
| `vmalert-service.yaml` | Service для UI vmalert |
| `alertmanager-values.yaml` | Helm-values Alertmanager: маршруты, Telegram, шаблон сообщения |
| `alertmanager-telegram-secret.yaml` | Реальный Secret с токеном (в `.gitignore`, не коммитится) |
| `alertmanager-telegram-secret.example.yaml` | Пример Secret с fake-токеном |
| `kustomization.yaml` | Сборка манифестов vmalert для `kubectl apply -k` |

`alertmanager-values.yaml` — это не Kubernetes-манифест, а values для Helm. Не применяйте его через `kubectl apply -f`. Реальный Secret с токеном лежит в `alertmanager-telegram-secret.yaml`; файл исключён из git, а `alertmanager-telegram-secret.example.yaml` содержит только fake-токен.

Дополнительно обновлены NetworkPolicy:

- `k8s/networkpolicies/50-vmsingle-network-policy.yaml` — ingress от vmalert к VictoriaMetrics;
- `k8s/networkpolicies/60-alertmanager-network-policy.yaml` — ingress от vmalert к Alertmanager;
- `k8s/networkpolicies/70-vmalert-network-policy.yaml` — egress vmalert к DNS, VictoriaMetrics и Alertmanager.

## Алерты

| Алерт | Метрика | Условие | Severity |
|---|---|---|---|
| `UrlServiceHighErrorRate` | `http_server_requests_seconds_count` | доля 5xx > 5% за 5м, 2м подряд | critical |
| `UrlTokenGenerationSlow` | `url_token_generation_duration_seconds_bucket` | p95 генерации токена > 1s за 5м, 2м подряд | warning |
| `JvmHeapUsageHigh` | `jvm_memory_used_bytes` / `jvm_memory_max_bytes` | heap > 85% в течение 5м | warning |
| `HikariConnectionPoolSaturation` | `hikaricp_connections_pending` | есть запросы в очереди пула 2м подряд | critical |

Из HTTP-метрик исключены `/actuator/*`, чтобы health-пробы Kubernetes не влияли на статистику.

## Перед применением

Замените в `alertmanager-values.yaml`:

- `chat_id` — на id чата, куда бот должен присылать уведомления.

Откройте `k8s/alerting/alertmanager-telegram-secret.yaml` и замените плейсхолдер:

```yaml
stringData:
  telegram-token: "PASTE_YOUR_TELEGRAM_BOT_TOKEN_HERE"
```

на реальный токен бота. Файл уже исключён из git через `.gitignore`.

Применить Secret:

```bash
kubectl apply -f k8s/alerting/alertmanager-telegram-secret.yaml
```

Альтернатива — создать Secret командой:

```bash
kubectl -n url-shortener-namespace create secret generic alertmanager-telegram \
  --from-literal=telegram-token='123456:AA...'
```

Токен выдаёт `@BotFather`. Чтобы узнать `chat_id`, напишите боту сообщение и выполните:

```bash
curl "https://api.telegram.org/bot<TOKEN>/getUpdates"
```

В ответе будет `message.chat.id`.

## Применение

Порядок:

```bash
kubectl apply -k k8s/alerting

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm upgrade --install alertmanager prometheus-community/alertmanager \
  -n url-shortener-namespace \
  -f k8s/alerting/alertmanager-values.yaml

kubectl apply -f k8s/networkpolicies/50-vmsingle-network-policy.yaml
kubectl apply -f k8s/networkpolicies/60-alertmanager-network-policy.yaml
kubectl apply -f k8s/networkpolicies/70-vmalert-network-policy.yaml
```

## Проверка

```bash
kubectl -n url-shortener-namespace get pods | grep -E 'vmalert|alertmanager'
kubectl -n url-shortener-namespace logs deploy/vmalert --tail=50
```

UI vmalert:

```bash
kubectl -n url-shortener-namespace port-forward svc/vmalert 8080:8080
```

Открыть `http://localhost:8080` — все четыре правила должны быть в состоянии `OK`.

UI Alertmanager:

```bash
kubectl -n url-shortener-namespace port-forward svc/alertmanager 9093:9093
```

Проверить, что метрики и бакеты действительно есть:

```bash
kubectl -n url-shortener-namespace exec deploy/url-service-deployment -- \
  wget -qO- http://localhost:8082/actuator/prometheus | \
  grep -E 'url_token_generation_duration_seconds_(bucket|count)|jvm_memory_(used|max)_bytes|hikaricp_connections_pending'
```

## Персистентность состояния алертов

vmalert по умолчанию хранит состояние алертов в памяти. В `vmalert-configmap.yaml` заданы:

- `-remoteWrite.url` — vmalert сохраняет состояние в VictoriaMetrics;
- `-remoteRead.url` — vmalert восстанавливает состояние после рестарта.

Оба указывают на `http://vmsingle-victoria-metrics-single-server:8428`.

Проверка: запустить алерт в firing, затем перезапустить vmalert:

```bash
kubectl -n url-shortener-namespace rollout restart deploy/vmalert
```

После рестарта алерт должен остаться в firing, а не начать заново с pending.

## Шаблон уведомления для смешанных групп

Alertmanager группирует алерты по `alertname` и `service`. В одной группе могут оказаться и firing, и resolved алерты. `.Status` в шаблоне описывает группу целиком, поэтому использовать его для заголовка нельзя: resolved-алерты окажутся под заголовком `FIRING`.

В `alertmanager-values.yaml` используются отдельные блоки:

```gotemplate
{{ if .Alerts.Firing }}🔥 FIRING ({{ len .Alerts.Firing }}){{ end }}
{{ if .Alerts.Resolved }}✅ RESOLVED ({{ len .Alerts.Resolved }}){{ end }}

{{ range .Alerts.Firing }}...{{ end }}
{{ range .Alerts.Resolved }}...{{ end }}
```

## Демонстрация срабатывания

### 1. UrlServiceHighErrorRate

Уронить `moderation-service` и создать поток кастомных ссылок:

```bash
kubectl -n url-shortener-namespace scale deploy/moderation-service-deployment --replicas=0

kubectl -n url-shortener-namespace port-forward svc/url-service 8080:80

for i in $(seq 1 300); do
  curl -s -o /dev/null -w '%{http_code}\n' \
    -X POST http://localhost:8080/urls/custom \
    -H 'Content-Type: application/json' \
    -d "{\"longUrl\":\"https://example.com/$i\",\"customShortCode\":\"err$i\"}"
done | sort | uniq -c
```

Вернуть сервис:

```bash
kubectl -n url-shortener-namespace scale deploy/moderation-service-deployment --replicas=2
```

### 2. UrlTokenGenerationSlow и HikariConnectionPoolSaturation

Дать нагрузку уникальными запросами:

```bash
for round in $(seq 1 30); do
  seq 1 500 | xargs -P 200 -I{} curl -s -o /dev/null \
    -X POST http://localhost:8080/urls \
    -H 'Content-Type: application/json' \
    -d "{\"longUrl\":\"https://example.com/load/$round/{}\"}"
done
```

Если пул не насыщается, временно уменьшить его размер:

```bash
kubectl -n url-shortener-namespace set env deploy/url-service-deployment \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=1

kubectl -n url-shortener-namespace rollout status deploy/url-service-deployment
```

После демонстрации вернуть:

```bash
kubectl -n url-shortener-namespace set env deploy/url-service-deployment \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE-
```

### 3. JvmHeapUsageHigh

Временно уменьшить heap и дать нагрузку:

```bash
kubectl -n url-shortener-namespace set env deploy/url-service-deployment \
  JAVA_TOOL_OPTIONS="-Xmx160m"

kubectl -n url-shortener-namespace rollout status deploy/url-service-deployment
```

После срабатывания алерта вернуть настройки:

```bash
kubectl -n url-shortener-namespace set env deploy/url-service-deployment JAVA_TOOL_OPTIONS-
kubectl -n url-shortener-namespace rollout status deploy/url-service-deployment
```

## Примечания

- Образ `vmalert` закреплён на `v1.154.0` — версия должна совпадать с версией `vmsingle`. Проверить можно так:

  ```bash
  kubectl -n url-shortener-namespace get deploy vmsingle-victoria-metrics-single-server \
    -o jsonpath='{.spec.template.spec.containers[0].image}'
  ```

- У vmalert нет отдельного конфигурационного файла для флагов. Значения лежат в `vmalert-configmap.yaml`, пробрасываются через `envFrom` и подставляются в `args` через `$(VAR)`.
- `UrlTokenGenerationSlow` использует гистограмму. Если в вашей версии приложения нет `url_token_generation_duration_seconds_bucket`, проверьте, что в `ShortTokenGenerator` стоит `@Timed(..., histogram = true)`.
- Реальный токен Telegram не должен попадать в git. `alertmanager-values.yaml` ссылается на файл `/etc/alertmanager/secrets/telegram-token`, который монтируется из Secret `alertmanager-telegram`.
