@echo off
echo Stopping ChatApp Microservices...

docker-compose -f docker-compose-microservices.yml down

echo.
echo All services stopped.