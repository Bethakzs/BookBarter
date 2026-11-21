// CI/CD пайплайн BookBarter
// GitHub push -> Build + Unit tests (JaCoCo) -> SonarQube + Quality Gate -> Docker build
//             -> Docker Hub -> Kubernetes (rolling update) -> Smoke test (автовідкат при збої)

// Сервіси, які пакуються в Docker-образи та розгортаються в Kubernetes.
// dir — папка в репозиторії (і назва образу), deployment — назва Deployment у k8s/*.yaml
// Основний профіль (k8s/bookbarter.yaml): реєстрація, авторизація, користувачі, книги, покупки
CORE_SERVICES = [
    [dir: 'eureka-service',       deployment: 'eureka'],
    [dir: 'api-gateway-service',  deployment: 'api-gateway'],
    [dir: 'auth-service',         deployment: 'auth-service'],
    [dir: 'user-service',         deployment: 'user-service'],
    [dir: 'book-service',         deployment: 'book-service'],
    [dir: 'purchase-service',     deployment: 'purchase-service'],
]
// Додаткові сервіси (k8s/optional-services.yaml) — розгортаються лише з параметром FULL_STACK=true
OPTIONAL_SERVICES = [
    [dir: 'notification-service', deployment: 'notification-service'],
    [dir: 'review-service',       deployment: 'review-service'],
    [dir: 'email-service',        deployment: 'email-service'],
]
// Усі Java-модулі (для звіту покриття); тести й SonarQube проходять для всіх, незалежно від профілю
JAVA_MODULES = ['eureka-service', 'api-gateway-service', 'auth-service', 'user-service', 'book-service',
                'purchase-service', 'notification-service', 'review-service', 'wishlist-service']

def deployedServices() {
    return params.FULL_STACK ? CORE_SERVICES + OPTIONAL_SERVICES : CORE_SERVICES
}

def expectedEurekaApps() {
    return deployedServices().findAll { !(it.deployment in ['eureka', 'email-service']) }
                             .collect { it.deployment.toUpperCase() }.join(' ')
}

def hasFrontend() {
    // фронтенд підключений як git submodule bookbarter-front (https://github.com/hryntar/BookBarter)
    return fileExists('bookbarter-front/package.json')
}

// Deployment-и, за якими стежимо при розгортанні та відкаті
def rolloutTargets() {
    return deployedServices() + (hasFrontend() ? [[dir: 'bookbarter-front', deployment: 'web']] : [])
}

def imageNames(services) {
    def names = services.collect { it.dir }
    if (hasFrontend()) { names << 'web' }
    return names
}

def rollback(services, namespace) {
    services.each { s ->
        sh "kubectl -n ${namespace} rollout undo deployment/${s.deployment} || true"
    }
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Публікувати образи й розгортати в Kubernetes')
        booleanParam(name: 'SONAR', defaultValue: true, description: 'Аналіз SonarQube + Quality Gate')
        booleanParam(name: 'FULL_STACK', defaultValue: false, description: 'Розгорнути також notification, review та email (Go) сервіси')
    }

    environment {
        REGISTRY_NAMESPACE = "${env.DOCKERHUB_USER ?: 'bethakzs'}"
        IMAGE_PREFIX       = "${REGISTRY_NAMESPACE}/bookbarter"
        K8S_NAMESPACE      = 'bookbarter'
        // Kubernetes Docker Desktop (kind): API і NodePort-и доступні з Jenkins через docker-мережу "kind"
        K8S_NETWORK        = "${env.K8S_NETWORK ?: 'kind'}"
        K8S_NODE           = "${env.K8S_NODE ?: 'desktop-control-plane'}"
        K8S_API_SERVER     = "${env.K8S_API_SERVER ?: 'https://desktop-control-plane:6443'}"
        KUBECONFIG         = "${env.WORKSPACE}/.kubeconfig"
        GATEWAY_URL        = "http://${K8S_NODE}:30080"
        EUREKA_URL         = "http://${K8S_NODE}:30761"
        WEB_URL            = "http://${K8S_NODE}:30173"
        MAVEN_ARGS         = '-B -ntp'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                // фронтенд — git submodule
                sh 'git submodule update --init --recursive'
                script {
                    env.GIT_SHORT = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_SHORT}"
                    currentBuild.description = "tag ${env.IMAGE_TAG}"
                }
                echo "Збірка ${env.IMAGE_TAG}"
            }
        }

        stage('Build & Unit tests') {
            steps {
                sh 'chmod +x mvnw && ./mvnw ${MAVEN_ARGS} clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    script {
                        recordCoverage(tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                                       id: 'jacoco', name: 'JaCoCo coverage',
                                       sourceDirectories: JAVA_MODULES.collect { [path: "${it}/src/main/java"] },
                                       sourceCodeRetention: 'LAST_BUILD')
                    }
                }
            }
        }

        stage('SonarQube analysis') {
            when { expression { params.SONAR } }
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh './mvnw ${MAVEN_ARGS} org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar -Dsonar.projectVersion=${IMAGE_TAG}'
                }
            }
        }

        stage('Quality Gate') {
            when { expression { params.SONAR } }
            steps {
                // SonarQube повідомляє результат через webhook http://jenkins:8080/sonarqube-webhook/
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker build') {
            steps {
                script {
                    deployedServices().each { s ->
                        sh "docker build -t ${IMAGE_PREFIX}-${s.dir}:${IMAGE_TAG} -t ${IMAGE_PREFIX}-${s.dir}:latest ${s.dir}"
                    }
                    if (hasFrontend()) {
                        sh "docker build -f frontend/Dockerfile -t ${IMAGE_PREFIX}-web:${IMAGE_TAG} -t ${IMAGE_PREFIX}-web:latest bookbarter-front"
                    }
                }
            }
        }

        stage('Publish images') {
            when { expression { params.DEPLOY } }
            steps {
                script {
                    def hasToken = false
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DH_USER', passwordVariable: 'DH_TOKEN')]) {
                        hasToken = sh(returnStatus: true, script: '[ -n "$DH_TOKEN" ]') == 0
                        if (hasToken) {
                            sh 'echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin'
                        }
                    }
                    if (hasToken) {
                        echo 'Публікація образів у Docker Hub'
                        imageNames(deployedServices()).each { name ->
                            sh "docker push ${IMAGE_PREFIX}-${name}:${IMAGE_TAG}"
                            sh "docker push ${IMAGE_PREFIX}-${name}:latest"
                        }
                    } else {
                        // Резервний режим для локального стенду: токен Docker Hub не задано (ci/.env -> DOCKERHUB_TOKEN),
                        // тому образи імпортуються напряму в containerd вузла Kubernetes (аналог "kind load docker-image")
                        echo 'DOCKERHUB_TOKEN не задано — образи завантажуються напряму у вузол Kubernetes'
                        imageNames(deployedServices()).each { name ->
                            sh "docker save ${IMAGE_PREFIX}-${name}:${IMAGE_TAG} | docker exec -i ${K8S_NODE} ctr -n k8s.io images import -"
                        }
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when { expression { params.DEPLOY } }
            steps {
                sh '''
                    # Jenkins і CI-Prometheus мають бачити кластер (API + NodePort-и) у docker-мережі kind
                    docker network connect "$K8S_NETWORK" jenkins 2>/dev/null || true
                    docker network connect "$K8S_NETWORK" ci-prometheus 2>/dev/null || true

                    # kubeconfig Docker Desktop вказує на 127.0.0.1 хоста — підміняємо адресу API для контейнера
                    kubectl --kubeconfig=/root/.kube/config config view --raw --minify --context=docker-desktop > "$KUBECONFIG"
                    kubectl config set-cluster "$(kubectl config view -o jsonpath='{.clusters[0].name}')" --server="$K8S_API_SERVER" >/dev/null
                    kubectl get nodes
                '''
                withCredentials([string(credentialsId: 'bookbarter-db-password', variable: 'DB_PASSWORD'),
                                 string(credentialsId: 'bookbarter-jwt-secret', variable: 'JWT_SECRET')]) {
                    sh '''
                        kubectl create namespace "$K8S_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
                        kubectl -n "$K8S_NAMESPACE" create secret generic bookbarter-secrets \
                            --from-literal=db-password="$DB_PASSWORD" \
                            --from-literal=jwt-secret="$JWT_SECRET" \
                            --dry-run=client -o yaml | kubectl apply -f -
                    '''
                }
                sh '''
                    # Job із тестовими даними перевиконується при кожному деплої
                    kubectl -n "$K8S_NAMESPACE" delete job book-seed --ignore-not-found
                    # Підставляємо в маніфест унікальний тег щойно зібраних образів (тому IfNotPresent достатньо)
                    sed -E -e "s#(image: [^ ]+/bookbarter-[a-z-]+):latest#\\1:${IMAGE_TAG}#" \
                           -e "s#imagePullPolicy: Always#imagePullPolicy: IfNotPresent#" \
                           k8s/bookbarter.yaml | kubectl apply -f -
                    kubectl apply -f k8s/prometheus.yaml
                    if [ "$FULL_STACK" = "true" ]; then
                        sed -E -e "s#(image: [^ ]+/bookbarter-[a-z-]+):latest#\\1:${IMAGE_TAG}#" \
                               -e "s#imagePullPolicy: Always#imagePullPolicy: IfNotPresent#" \
                               k8s/optional-services.yaml | kubectl apply -f -
                    else
                        # Мінімальний профіль: додаткові сервіси прибираються, щоб звільнити пам'ять
                        kubectl delete -f k8s/optional-services.yaml --ignore-not-found
                    fi
                    if [ -f bookbarter-front/package.json ]; then
                        sed -E -e "s#(image: [^ ]+/bookbarter-web):latest#\\1:${IMAGE_TAG}#" \
                               -e "s#imagePullPolicy: Always#imagePullPolicy: IfNotPresent#" \
                               k8s/frontend.yaml | kubectl apply -f -
                    fi
                '''
                script {
                    rolloutTargets().each { s ->
                        sh "kubectl -n ${K8S_NAMESPACE} rollout status deployment/${s.deployment} --timeout=600s"
                    }
                }
            }
            post {
                failure {
                    echo 'Розгортання не вдалося — відкат до попередньої версії'
                    sh 'kubectl -n "$K8S_NAMESPACE" get pods -o wide || true'
                    script { rollback(rolloutTargets(), env.K8S_NAMESPACE) }
                }
            }
        }

        stage('Smoke test') {
            when { expression { params.DEPLOY } }
            environment {
                // Spring-сервіси, які мають зареєструватися в Eureka (назва Deployment = spring.application.name)
                EXPECTED_APPS = expectedEurekaApps()
            }
            steps {
                sh '''
                    echo "Перевірка API Gateway: $GATEWAY_URL/actuator/health"
                    for i in $(seq 1 30); do
                        if curl -fsS "$GATEWAY_URL/actuator/health" | grep -q '"status":"UP"'; then
                            echo "Gateway UP"; break
                        fi
                        [ "$i" = 30 ] && { echo "Gateway не відповідає"; exit 1; }
                        sleep 10
                    done

                    echo "Очікуємо реєстрації в Eureka: $EXPECTED_APPS"
                    for i in $(seq 1 24); do
                        apps=$(curl -fsS -H 'Accept: application/json' "$EUREKA_URL/eureka/apps" | grep -o '"app":"[A-Z-]*"' | cut -d'"' -f4 | sort -u)
                        missing=""
                        for a in $EXPECTED_APPS; do echo "$apps" | grep -qx "$a" || missing="$missing $a"; done
                        if [ -z "$missing" ]; then echo "Усі сервіси зареєстровані в Eureka:"; echo "$apps"; break; fi
                        [ "$i" = 24 ] && { echo "Не зареєструвались:$missing"; exit 1; }
                        sleep 10
                    done

                    echo "Захищений ендпоінт без JWT має повертати 401/403:"
                    code=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/api/book/get-all")
                    echo "GET /api/book/get-all -> HTTP $code"
                    case "$code" in 401|403) ;; *) echo "Неочікуваний код відповіді"; exit 1 ;; esac

                    if [ -f bookbarter-front/package.json ]; then
                        echo "Фронтенд: $WEB_URL"
                        curl -fsS "$WEB_URL/" | grep -q 'id="root"' && echo "Фронтенд віддає index.html (React)" || { echo "Фронтенд не відповідає"; exit 1; }
                    fi
                '''
            }
            post {
                failure {
                    echo 'Smoke test не пройдено — відкат до попередньої версії'
                    script { rollback(rolloutTargets(), env.K8S_NAMESPACE) }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout >/dev/null 2>&1 || true'
        }
        success {
            echo "Версія ${env.IMAGE_TAG} успішно розгорнута в Kubernetes"
        }
        failure {
            echo "Пайплайн завершився з помилкою на збірці ${env.IMAGE_TAG}"
        }
    }
}
