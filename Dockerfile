FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# GitHub Actions에서 미리 빌드된 JAR 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=60.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
