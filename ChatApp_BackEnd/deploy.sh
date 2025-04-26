#!/bin/bash
set -e

APP_DIR=~/chatapp
cd $APP_DIR

# For the first time, you need to build all the images manually
echo "Pulling latest images for api-gateway and backend..."
docker-compose pull fronttend backend

echo "Restarting api-gateway and backend services..."
docker-compose up -d --no-deps frontend backend

# 简单的健康检查
echo "Performing health check..."
sleep 10
if curl -s http://localhost:80 > /dev/null; then
  echo "API Gateway is responding correctly"
else
  echo "WARNING: API Gateway health check failed"
fi

echo "Deployment completed at $(date)"
echo "Cleaning up old images..."
docker image prune -f