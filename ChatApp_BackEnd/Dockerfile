# FROM openjdk:17-jdk-slim
# WORKDIR /chatApp
# COPY target/*.jar chatApp.jar

# # ENV DB_HOST=192.168.86.128 \
# #     DB_PORT=5432 \
# #     DB_NAME=chatAppDB \
# #     DB_USERNAME=postgres \
# #     DB_PASSWORD=721215 \
# #     MONGO_HOST=192.168.86.128 \
# #     MONGO_PORT=27017 \
# #     MONGO_DBNAME=ChatApp

# EXPOSE 53050
# ENTRYPOINT ["java","-jar","chatApp.jar"]

FROM maven:3.9.9 AS build
WORKDIR /app
COPY pom.xml .
# 下载依赖项（这一步会被缓存，除非pom发生变化）
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /chatApp
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY --from=build /app/target/*.jar chatApp.jar
EXPOSE 53050
ENTRYPOINT ["java", "-jar", "chatApp.jar"]