FROM openjdk:17-jdk-slim

LABEL maintainer="devStudy Team"
LABEL service="websocket-gateway"
LABEL version="1.0.0"

WORKDIR /app

COPY target/websocket-gateway-*.jar app.jar

EXPOSE 8086

# 设置JVM参数
ENV JAVA_OPTS="-Xmx512m -Xms256m"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]