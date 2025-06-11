# 1. 빌드 스테이지 (Java 21 JDk)
FROM openjdk:21-jdk-slim AS builder
WORKDIR /workspace/app

COPY build.gradle settings.gradle /workspace/app/
COPY gradlew /workspace/app/
COPY gradle /workspace/app/gradle
COPY src /workspace/app/src

# gradle wrapper 사용


# gradlew로 빌드 (테스트 생략)
RUN chmod +x ./gradlew && ./gradlew build -x test

# 2. 런타임 스테이지 (Java 21 JRE)
FROM openjdk:21-jre-slim
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]