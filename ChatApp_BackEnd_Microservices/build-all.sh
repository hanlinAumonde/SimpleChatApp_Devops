#!/bin/bash

# 构建所有微服务的脚本
set -e

echo "开始构建ChatApp微服务..."

# 构建Eureka Server
echo "构建 Eureka Server..."
cd eureka-server
mvn clean package -DskipTests
cd ..

# 构建API Gateway
echo "构建 API Gateway..."
cd api-gateway
mvn clean package -DskipTests
cd ..

# 构建Config Server (暂时禁用)
# echo "构建 Config Server..."
# cd config-server
# mvn clean package -DskipTests
# cd ..

echo "所有基础设施服务构建完成!"

# 启动Docker Compose
echo "启动微服务环境..."
# docker-compose -f docker-compose-microservices.yml up -d
docker-compose -f docker-compose-microservices.yml up -d api-gateway eureka-server redis

echo "微服务环境启动完成!"
echo "Eureka Server: http://localhost:8761"
echo "API Gateway: http://localhost:8080"
# echo "Config Server: http://localhost:8888" (暂时禁用)