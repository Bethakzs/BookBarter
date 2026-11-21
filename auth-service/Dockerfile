# Runtime-образ сервісу. JAR збирається заздалегідь (у Jenkins: ./mvnw verify),
# тож образ лише пакує вже протестований артефакт — принцип "build once, deploy many".
# Локально: з кореня репозиторію  .\mvnw.cmd -DskipTests package  , потім docker compose up --build
FROM eclipse-temurin:21-jre-jammy

RUN useradd --system --uid 1001 app
WORKDIR /app
COPY target/*.jar app.jar
USER app

# Менший відбиток пам'яті та швидший старт у Kubernetes
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k"

ENTRYPOINT ["java", "-jar", "app.jar"]
