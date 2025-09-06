FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/websocket-gateway-*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]