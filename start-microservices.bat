@echo off
echo Starting ChatApp Microservices...

:: 创建日志目录
mkdir logs\auth-service 2>nul
mkdir logs\api-gateway 2>nul
mkdir logs\crud-service 2>nul
mkdir logs\message-service 2>nul
mkdir logs\websocket-gateway 2>nul

:: 启动所有服务
docker-compose -f docker-compose-microservices.yml up -d

echo.
echo Services are starting up...
echo.
echo Web interfaces:
echo - Eureka Server: http://localhost:8761
echo - API Gateway: http://localhost:8080
echo - Prometheus: http://localhost:9090
echo - Grafana: http://localhost:3000 (admin/admin)
echo - Zipkin: http://localhost:9411
echo - RabbitMQ: http://localhost:15672 (admin/admin)
echo.
echo Microservices:
echo - Auth Service: http://localhost:8081/actuator/health
echo - CRUD Service: http://localhost:8083/actuator/health
echo - Message Service: http://localhost:8084/actuator/health
echo - WebSocket Gateway: http://localhost:8086/actuator/health
echo.
echo To view logs: docker-compose -f docker-compose-microservices.yml logs -f [service-name]
echo To stop all: docker-compose -f docker-compose-microservices.yml down