# BookBarter — CI/CD

Пайплайн: **GitHub → Jenkins → Maven (тести + JaCoCo) → SonarQube Quality Gate → Docker → Docker Hub → Kubernetes → Smoke test → Prometheus/Grafana**.

| Інструмент | Роль | Де налаштовано |
|---|---|---|
| Jenkins | оркестрація пайплайну (обов'язковий інструмент) | `ci/jenkins/` (Dockerfile, plugins.txt, casc.yaml), `Jenkinsfile` |
| Maven + JUnit 5 + Mockito + JaCoCo | збірка, юніт-тести, покриття | `pom.xml` (агрегатор), `*/pom.xml` |
| SonarQube | статичний аналіз, покриття, Quality Gate | `ci/docker-compose.yml` |
| Docker | образи сервісів | `*/Dockerfile` |
| Docker Hub | реєстр образів (артефакти) | `bethakzs/bookbarter-<service>:<build>-<sha>` |
| Kubernetes (Docker Desktop) | середовище розгортання | `k8s/bookbarter.yaml`, `k8s/prometheus.yaml` |
| Prometheus + Grafana | моніторинг сервісів і самого CI | `k8s/prometheus.yaml`, `ci/prometheus/`, `ci/grafana/` |

## 1. Передумови

- Docker Desktop з увімкненим Kubernetes: *Settings → Kubernetes → Enable Kubernetes*.
- Пам'ять: стенд (Jenkins + SonarQube + Kafka + 8 Java-сервісів) потребує ~10–11 GB для Docker.
  За замовчуванням WSL2 дає Docker лише половину RAM. Створи `%USERPROFILE%\.wslconfig`:
  ```ini
  [wsl2]
  memory=11GB
  ```
  і перезапусти: `wsl --shutdown`, потім знову відкрий Docker Desktop.
- Акаунт Docker Hub + Personal Access Token (Read & Write).

## 2. Запуск інфраструктури CI

```powershell
cd ci
copy .env.example .env      # заповнити значення (див. нижче)
docker compose up -d --build
```

| Сервіс | URL | Логін |
|---|---|---|
| Jenkins | http://localhost:8080 | `JENKINS_ADMIN_ID` / `JENKINS_ADMIN_PASSWORD` з `ci/.env` |
| SonarQube | http://localhost:9000 | admin / admin, далі новий пароль (`SONAR_ADMIN_PASSWORD` в `ci/.env`) |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` з `ci/.env` |

**Що має працювати в момент push:** Docker Desktop з увімкненим Kubernetes і контейнери `ci/`.
Сам застосунок вручну не запускається: його розгортає в Kubernetes пайплайн (етап Deploy).
Якщо Jenkins був вимкнений під час push, він збере пропущений коміт після запуску (Poll SCM).
Фронтенд — git submodule `bookbarter-front` (https://github.com/hryntar/BookBarter), образ збирається з `frontend/Dockerfile`.
Після клонування: `git submodule update --init`.
Кореневий `docker-compose.yml` — старий спосіб локального запуску без Kubernetes, для CI/CD не потрібен.

## 3. Одноразове налаштування

1. **SonarQube пароль**: при першому вході admin/admin SonarQube вимагає новий пароль (мінімум 12 символів).
2. **SonarQube токен**: SonarQube → *My Account → Security → Generate Token* (тип *Global Analysis Token*) → вставити в `ci/.env` як `SONAR_TOKEN`.
3. **SonarQube webhook** (щоб Jenkins отримав результат Quality Gate):
   - *Administration → Configuration → General → Security*: вимкнути **Enable local webhooks validation** (`sonar.validateWebhooks=false`), бо Jenkins — сусідній контейнер у локальній мережі;
   - *Administration → Configuration → Webhooks → Create*: name `jenkins`, URL `http://jenkins:8080/sonarqube-webhook/`.
4. **Docker Hub токен**: вставити в `ci/.env` як `DOCKERHUB_TOKEN`.
   Поки токен порожній, пайплайн не публікує образи, а імпортує їх напряму у вузол Kubernetes (`docker save | ctr images import`) — зручно для локального тесту.
5. Застосувати зміни `.env`: `docker compose up -d jenkins` (Jenkins перечитає конфігурацію JCasC).

**Kubernetes Docker Desktop** працює як kind-кластер у docker-мережі `kind`. Пайплайн сам підключає контейнери `jenkins` і `ci-prometheus` до цієї мережі та звертається до API за адресою `https://desktop-control-plane:6443` (змінюється через `K8S_API_SERVER` у `ci/.env`).

Усі налаштування Jenkins (адмін, credentials, SonarQube-сервер, Prometheus-метрики, job `bookbarter`) створюються автоматично з `ci/jenkins/casc.yaml` — вручну в UI нічого клікати не треба.

## 4. Як працює пайплайн (`Jenkinsfile`)

| Етап | Що робить | Падає, якщо… |
|---|---|---|
| Checkout | клонує репозиторій, формує тег `<BUILD_NUMBER>-<git sha>` | — |
| Build & Unit tests | `./mvnw clean verify` для всіх 9 Java-сервісів, звіти JUnit і JaCoCo в Jenkins | не компілюється або падає тест |
| SonarQube analysis | аналіз коду + покриття | — |
| Quality Gate | чекає вердикт SonarQube | код не проходить Quality Gate |
| Docker build | образи всіх сервісів (+ фронт, якщо є `bookbarter-front/package.json`) | помилка в Dockerfile |
| Publish images | Docker Hub: `:<tag>` і `:latest`; без токена — імпорт у вузол Kubernetes | невірний токен Docker Hub |
| Deploy to Kubernetes | Secret з паролями, `kubectl apply` з конкретним тегом, `rollout status` | под не стартує за 10 хв → **автовідкат** (`rollout undo`) |
| Smoke test | `/actuator/health` API Gateway, список сервісів в Eureka, захищений ендпоінт без JWT → 401/403 | Gateway не UP → **автовідкат** |

Тригери збірки:
- **git-хук `ci/hooks/pre-push`** — одразу після `git push` у гілку з `GIT_BRANCH` дочікується, поки GitHub прийме коміт,
  і запускає job у локальному Jenkins (`buildWithParameters` з токеном із JCasC). Збірка стартує за ~10 с
  з приміткою «Started by remote host ... with note: git push <коміт>». Jenkins при цьому не відкривається в інтернет.
  Увімкнути один раз у клоні репозиторію: `git config core.hooksPath ci/hooks`.
- **Poll SCM кожні 2 хв** — резервний тригер (push з іншої машини, через веб-інтерфейс GitHub, або Jenkins був вимкнений).
- GitHub webhook (`/github-webhook/`) теж підтримується job'ом, але потребує публічної адреси Jenkins (ngrok, smee.io тощо).

Параметри збірки:
- `DEPLOY=false` — лише CI (збірка, тести, Sonar, образи) без публікації й розгортання;
- `SONAR=false` — пропустити SonarQube (швидка збірка);
- `FULL_STACK=true` — розгорнути також додаткові сервіси з `k8s/optional-services.yaml` (notification, review, email на Go).
  За замовчуванням розгортається **мінімальний профіль** — eureka, api-gateway, auth, user, book, purchase + Kafka і PostgreSQL —
  щоб стенд вміщався в пам'ять ноутбука. Тести й аналіз SonarQube виконуються для всіх модулів незалежно від профілю.
- `FORCE_ALL=true` — перезібрати й передеплоїти всі сервіси, навіть без змін у коді.

**Інкрементальний деплой.** Тег образу — хеш вихідного коду сервісу в git (`src-<hash>` = `git rev-parse HEAD:<папка сервісу>`;
для фронту — коміт submodule + `frontend/Dockerfile`). Етап **Detect changes** порівнює його з образом, що зараз працює
в Kubernetes, і друкує план `BUILD`/`skip`. Збираються, публікуються в Docker Hub і перезапускаються лише змінені сервіси;
решта зберігає свій образ, і `kubectl apply` їх не чіпає. Якщо деплой або smoke test падає — відкочуються тільки змінені.
Тести й SonarQube все одно проходять для всього коду.

### Як швидко перевірити зміну бекенду
1. **Версія Gateway.** У `api-gateway-service/src/main/resources/application.yml` зміни `info.app.version` (напр. `1.0.1`),
   commit + push. План збірки покаже `BUILD api-gateway`, решта — `skip`. Результат: http://localhost:8000/actuator/info.
2. **Який образ працює:** `kubectl -n bookbarter get deploy -o wide` (колонка IMAGES — тег змінився лише в зміненого сервісу),
   `kubectl -n bookbarter rollout history deployment/api-gateway`.
3. **Зміна логіки + API.** Напр., у `auth-service/.../service/UserService.java` новому користувачу `.bucks(5L)` → `.bucks(10L)`,
   push, потім зареєструватися на фронті (http://localhost:4173) і перевірити баланс у профілі.
4. **Логи нового пода:** `kubectl -n bookbarter logs deploy/book-service --tail=50` (або `-f` в реальному часі).
5. **Зламана зміна:** зламай тест сервісу — збірка зупиниться на тестах, у кластері лишиться попередня версія.

## 5. Моніторинг

- Prometheus у Kubernetes (`k8s/prometheus.yaml`, NodePort 30090) збирає `/actuator/prometheus` з усіх сервісів.
- Kubernetes Docker Desktop (kind) не публікує NodePort-и на localhost Windows — для браузера використовуй `k8s/port-forward.ps1`.
- Prometheus у `ci/` збирає метрики Jenkins (`/prometheus/`) і федерує метрики сервісів з Kubernetes.
- Grafana (папка *BookBarter*): дашборди **«Jenkins CI/CD»** (кількість, тривалість і успішність збірок) та **«BookBarter — мікросервіси»** (UP/DOWN, req/s, латентність, 5xx, JVM).

## 6. Корисні команди

```powershell
powershell -ExecutionPolicy Bypass -File k8s/port-forward.ps1   # Фронт :4173, Gateway :8000, Eureka :8761, Prometheus кластера :9091
kubectl -n bookbarter get pods
kubectl -n bookbarter rollout history deployment/book-service
kubectl -n bookbarter rollout undo deployment/book-service   # ручний відкат
docker compose -f ci/docker-compose.yml logs -f jenkins
```

Локальна збірка без Jenkins: `.\mvnw.cmd -DskipTests package` у корені, далі `docker compose up --build`.
