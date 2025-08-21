# ChatApp 微服务基础设施

## 项目结构

```
ChatApp_BackEnd_Microservices/
├── eureka-server/                  # 服务注册中心 (8761)
├── api-gateway/                    # API网关 (8080)
├── config-server/                  # 配置中心 (8888)
├── auth-service/                   # 认证服务 (8081) - 后续添加
├── user-service/                   # 用户服务 (8082) - 后续添加
├── chatroom-service/               # 聊天室服务 (8083) - 后续添加
├── message-service/                # 消息服务 (8084) - 后续添加
├── notification-service/           # 通知服务 (8085) - 后续添加
├── websocket-gateway/              # WebSocket网关 (8086) - 后续添加
├── docker-compose-microservices.yml
├── build-all.sh (Linux/Mac)
├── build-all.bat (Windows)
└── README.md
```

## 快速启动

### 1. 构建并启动基础设施

**Windows:**
```bash
# 进入微服务目录
cd ChatApp_BackEnd_Microservices

# 运行构建脚本
build-all.bat
```

**Linux/Mac:**
```bash
# 进入微服务目录
cd ChatApp_BackEnd_Microservices

# 添加执行权限
chmod +x build-all.sh

# 运行构建脚本
./build-all.sh
```

### 2. 手动启动方式

```bash
# 1. 构建各个服务
cd eureka-server && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..
cd config-server && mvn clean package -DskipTests && cd ..

# 2. 启动Docker Compose
docker-compose -f docker-compose-microservices.yml up -d

# 3. 查看服务状态
docker-compose -f docker-compose-microservices.yml ps
```

## 服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Eureka Server | http://localhost:8761 | 服务注册中心管理界面 |
| API Gateway | http://localhost:8080 | API网关入口 |
| Config Server | http://localhost:8888 | 配置中心 |
| RabbitMQ Management | http://localhost:15672 | 消息队列管理 (admin/admin) |

## 验证步骤

### 1. 检查Eureka Server
访问 http://localhost:8761，应该看到：
- Eureka管理界面
- 注册的服务列表（api-gateway, config-server）

### 2. 检查API Gateway
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 网关路由信息
curl http://localhost:8080/actuator/gateway/routes
```

### 3. 检查Config Server
```bash
# 健康检查
curl http://localhost:8888/actuator/health

# 获取应用配置
curl http://localhost:8888/application/default
```

### 4. 检查服务注册
在Eureka界面中确认以下服务已注册：
- API-GATEWAY
- CONFIG-SERVER

## 常见问题

### Q1: 服务启动失败
**解决方案:**
1. 检查端口是否被占用
2. 确保Docker正在运行
3. 检查Maven环境是否配置正确

### Q2: 服务无法注册到Eureka
**解决方案:**
1. 检查网络连接
2. 确认Eureka Server已启动
3. 查看服务日志：`docker-compose logs [service-name]`

### Q3: API Gateway路由失败
**解决方案:**
1. 确认目标服务已在Eureka注册
2. 检查Gateway配置中的路由规则
3. 查看Gateway日志

## 下一步

基础设施搭建完成后，可以开始第二阶段：
1. 创建认证服务 (auth-service)
2. 从单体应用迁移认证相关功能
3. 测试通过API Gateway的认证流程

## 清理环境

```bash
# 停止所有服务
docker-compose -f docker-compose-microservices.yml down

# 清理数据卷（谨慎使用）
docker-compose -f docker-compose-microservices.yml down -v

# 清理Docker镜像
docker system prune -a
```