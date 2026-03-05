FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# GitHub Actions에서 미리 빌드된 JAR 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=55.0", \
  "-XX:InitialRAMPercentage=25.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:MaxMetaspaceSize=192m", \
  "-XX:MetaspaceSize=96m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-Xss512k", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Duser.timezone=Asia/Seoul", \
  "-jar", "app.jar"]
