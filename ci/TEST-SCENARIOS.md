# Сценарії тестування CI/CD (розділ «Тестування та валідація»)

Перед початком: запущені `ci/` (`docker compose -f ci/docker-compose.yml up -d`), Kubernetes у Docker Desktop,
і (для перегляду в браузері) `powershell -ExecutionPolicy Bypass -File k8s\port-forward.ps1`.

Jenkins: http://localhost:8080 → job **BookBarter CI/CD**. Після `git push` збірка стартує за кілька секунд
(git-хук `ci/hooks/pre-push`, увімкнений через `git config core.hooksPath ci/hooks`; причина — «Started by an SCM change»).
Запустити вручну без push — **Build with Parameters → Build**.

Для кожного сценарію зафіксуй: номер збірки, на якому етапі зупинилась, тривалість, скріни.

---

## 1. Успішна зміна: push → автоматичний деплой

1. Безпечна зміна — наприклад, закомітити нові файли CI (git-хук і ці сценарії):
2. ```powershell
   git add ci
   git commit -m "ci: instant build trigger after push, test scenarios"
   git push
   ```
3. **Очікується:** збірка зелена, усі етапи пройдені; в Docker Hub з'явився новий тег `<номер>-<sha>`.
4. **Перевірка:**
   ```powershell
   kubectl -n bookbarter get pods
   kubectl -n bookbarter get deploy -o wide        # колонка IMAGES — новий тег
   ```
   Фронтенд — http://localhost:4173; Eureka (http://localhost:8761) — 5 сервісів; https://hub.docker.com/u/bethakzs — образи з тегом.
5. **Скріни:** Stage View у Jenkins, Docker Hub, `kubectl get pods`, Eureka.

## 2. Зламаний юніт-тест → деплой блокується

1. У `book-service/src/test/java/com/example/bookservice/service/BookServiceImplTest.java`
   (тест `saveBook_mapsDtoToAvailableBookOwnedByCaller`) заміни
   `isEqualTo("Dune")` на `isEqualTo("Dune 2")`.
2. `git commit -am "test: broken unit test scenario"` і `git push`.
3. **Очікується:** червоний етап **Build & Unit tests**, наступні етапи пропущені (Skipped),
   у кластері далі працює попередня версія. На сторінці збірки — **Test Result: 1 failure** з назвою тесту.
4. **Повернути:** `git revert --no-edit HEAD` і `git push` (наступна збірка знову зелена).
5. **Скріни:** червоний Stage View, Test Result з упалим тестом.

## 3. Поганий код → SonarQube Quality Gate зупиняє пайплайн

1. SonarQube (http://localhost:9000) → **Quality Gates → Create** → назва `BookBarter strict`
   → **Add Condition** → *On Overall Code* → **Coverage** is less than **80** → **Set as Default**.
2. Jenkins → **Build with Parameters → Build** (код не змінюємо).
3. **Очікується:** етап **Quality Gate** червоний: «Pipeline aborted due to quality gate failure: ERROR»;
   Docker build і деплой не виконуються. У SonarQube проєкт BookBarter — статус **Failed**
   (покриття ~36% < 80%).
4. **Повернути:** Quality Gates → **Sonar way** → **Set as Default**, ще раз Build.
5. **Скріни:** червоний етап Quality Gate, SonarQube Overview з Failed і умовою.

## 4. Регресія після деплою → smoke test + автоматичний відкат

1. У `api-gateway-service/src/main/java/com/example/apigatewayservice/security/SecurityConfig.java`
   в список `permitAll()` додай `"/api/book/**",` (тобто захищений API стає публічним — регресія безпеки).
2. `git commit -am "test: security regression scenario"` і `git push`.
3. **Очікується:** деплой проходить, але **Smoke test** падає:
   `GET /api/book/get-all -> HTTP 200` → «Неочікуваний код відповіді» →
   «Smoke test не пройдено — відкат до попередньої версії» → `rollout undo` для всіх сервісів.
4. **Перевірка відкату:**
   ```powershell
   kubectl -n bookbarter rollout history deployment/api-gateway
   kubectl -n bookbarter get deploy api-gateway -o wide      # IMAGES — знову попередній тег
   ```
5. **Повернути:** `git revert --no-edit HEAD` і `git push`.
6. **Скріни:** лог етапу Smoke test з відкатом, `rollout history`.

## 5. Самовідновлення Kubernetes і моніторинг

1. ```powershell
   kubectl -n bookbarter delete pod -l app=book-service
   kubectl -n bookbarter get pods -w        # новий под створюється автоматично (Ctrl+C — вийти)
   ```
2. Grafana (http://localhost:3000 → BookBarter → «Мікросервіси»): на панелі **Uptime** book-service падає до 0
   і росте знову; таблиця **Стан сервісів** — короткочасно 0.
3. **Скріни:** `get pods -w`, панель Uptime у Grafana.

## 6. Ручний відкат версії

```powershell
kubectl -n bookbarter rollout history deployment/book-service
kubectl -n bookbarter rollout undo deployment/book-service
kubectl -n bookbarter rollout status deployment/book-service
```

---

## Дані для розділу «Оцінка ефективності»

- Тривалість збірок і етапів: Jenkins → job → **Stage View**; Grafana → «Jenkins CI/CD»
  (тривалість останньої збірки, етапи, стабільність, кількість успішних/невдалих).
- Тести: 98 юніт-тестів, покриття JaCoCo (Jenkins → **Coverage Report**), SonarQube → Overview
  (Bugs, Vulnerabilities, Code Smells, Coverage, Duplications).
- Порівняння: ручний деплой (збірка 9 сервісів, `docker build`, `docker push`, `kubectl apply` вручну,
  перевірка) — оцінити час самому, vs ~6–8 хв автоматично без участі людини; помилки ловляться
  до деплою (сценарії 2–3) або відкочуються автоматично (сценарій 4).
