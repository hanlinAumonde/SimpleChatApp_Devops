# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个基于微服务架构的实时聊天应用程序，采用前后端分离设计：

- **后端**: Spring Boot 3.3.0 + Java 17，提供 REST API 和 WebSocket 支持
- **前端**: Angular 19，提供用户界面
- **数据存储**: PostgreSQL (用户和聊天室数据) + MongoDB (聊天消息历史)
- **中间件**: Redis (缓存和分布式会话管理) + RabbitMQ (消息队列)
- **部署**: Docker Compose + GitHub Actions CI/CD

## 开发命令

### 后端 (Spring Boot)
```bash
# 开发环境运行
cd ChatApp_BackEnd
./mvnw spring-boot:run

# 构建
./mvnw clean package

# 运行测试
./mvnw test

# 构建 Docker 镜像
docker build -t chatapp-backend .
```

### 前端 (Angular)
```bash
# 安装依赖
cd ChatApp_FrontEnd
npm install

# 开发服务器
npm run start
# 或
ng serve

# 生产构建
npm run build
# 或
ng build --configuration production

# 单元测试
npm run test
# 或
ng test

# E2E 测试 (Cypress)
npm run cypress:open  # 交互模式
npm run cypress:run   # 命令行模式
```

### 容器化部署
```bash
# 本地完整部署
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f [service-name]

# 重建特定服务
docker-compose up -d --build [service-name]
```

## 架构要点

### 核心业务流程
- **身份验证**: 支持用户名/密码登录和邮箱验证码登录，JWT token 管理
- **聊天室管理**: 用户可创建、加入、修改聊天室，支持邀请其他用户
- **实时通信**: WebSocket 连接，支持分布式部署时的消息广播
- **消息持久化**: PostgreSQL 存储用户和聊天室信息，MongoDB 存储消息历史

### 分布式支持
- 使用 Redis pub/sub 实现分布式 WebSocket 会话管理
- RabbitMQ 处理聊天室成员变更等异步事件
- Spring Security Remember-Me 功能支持持久化会话

### 关键配置
- **后端端口**: 53050
- **前端端口**: 4200 (开发), 80/443 (生产)
- **数据库**: PostgreSQL (chatAppDB), MongoDB (ChatApp)
- **缓存**: Redis 用于验证码和黑名单缓存 (5分钟过期)

### 项目结构
```
ChatApp_BackEnd/src/main/java/com/devStudy/chat/
├── config/          # Spring 配置 (Security, WebSocket, Redis, RabbitMQ)
├── controller/      # REST API 控制器
├── dao/            # JPA Repository 接口
├── dto/            # 数据传输对象和映射器
├── model/          # JPA 实体类
├── security/       # 安全认证和授权逻辑
├── service/        # 业务逻辑服务层
├── websocket/      # WebSocket 处理和分布式支持
└── utils/          # 工具类和异常定义

ChatApp_FrontEnd/src/app/
├── ChatAppComponents/     # 主要页面组件
├── CommonComponents/      # 通用UI组件
├── LoginComponents/       # 登录相关组件
├── Models/               # TypeScript 模型定义
├── Services/             # Angular 服务
└── RouteGuards/          # 路由守卫
```

## 环境变量

关键环境变量 (docker-compose.yml 中定义):
- `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`: PostgreSQL 连接
- `MONGO_HOST`: MongoDB 连接
- `REDIS_HOST`: Redis 连接
- `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`: RabbitMQ 连接
- `FRONT_URL`: 前端应用地址 (用于 CORS 配置)

## CI/CD 部署

使用 GitHub Actions 自动化部署到 EC2：
1. 构建并推送 Docker 镜像到 Docker Hub
2. SSH 连接到 EC2 实例
3. 执行 `deploy.sh` 脚本更新服务
4. 执行健康检查确认部署成功

## 后端具体解析

### 后端项目详细解析

1. 配置层 (Config)

`WebSecurityConfig.java`

- 双重认证机制: 支持用户名/密码登录和邮箱验证码登录
- JWT Token管理: 无状态会话，基于Cookie的JWT认证
- 黑名单机制: 失效的JWT token会被加入Redis黑名单
- CSRF防护: 使用SpaCsrfTokenRequestHandler处理SPA应用的CSRF
- Remember-Me功能: 基于数据库的持久化记住我功能
- 权限控制: /api/users/**和/api/chatrooms/**需要USER角色，登录相关API公开访问

`WebSocketConfig.java`

- 分布式WebSocket: 使用DistributedChatWebSocketHandler支持多实例部署
- 握手拦截: ChatHandShakeInterceptor验证用户身份和聊天室权限
- 路径映射: WebSocket端点/ws/chatroom/{chatroomId}/user/{userId}

`RedisConfig.java`

- 消息监听: 配置Redis pub/sub用于分布式WebSocket消息广播
- 序列化: 使用Jackson JSON序列化存储对象

2. 控制器层 (Controller)

`LoginManageController.java (/api/login)`

- GET /check-login: 验证JWT token并返回用户信息
- GET /verification-code: 发送邮箱验证码，通过RabbitMQ异步处理
- POST /forget-password: 忘记密码，发送重置邮件
- POST /logout: 登出，将JWT加入黑名单
- POST /compte/create: 用户注册

`UserManageController.java (/api/users)`

- GET /others: 分页获取其他用户（用于聊天室邀请）
- GET /{userId}/chatrooms/owned: 获取用户创建的聊天室
- GET /{userId}/chatrooms/joined: 获取用户加入的聊天室

`ChatroomManageController.java (/api/chatrooms)`

- POST /create: 创建聊天室
- DELETE /{chatroomId}: 删除聊天室
- PUT /modify: 修改聊天室信息
- GET /{chatroomId}/users: 获取聊天室成员
- GET /{chatroomId}/messages: 获取聊天消息历史（分页）

3. 业务服务层 (Service)

`UserService.java`

- 实现UserDetailsService接口，集成Spring Security
- 用户CRUD操作，密码加密（BCrypt）
- JWT token生成和验证
- 分页查询和用户权限管理

`ChatroomService.java`

- 聊天室生命周期管理（创建、修改、删除）
- 成员管理（邀请、移除）
- 发布领域事件（ChangeChatroomMemberEvent、RemoveChatroomEvent）
- 与用户服务协同验证权限

`ChatMessageService.java`

- 消息持久化到MongoDB
- 分页查询聊天历史
- 消息格式化和时间戳处理

4. 安全认证机制

`多重认证过滤器链`

1. JwtAuthenticationFilter: 验证Cookie中的JWT token，自动登录有效用户
2. VerificationCodeAuthenticationFilter: 处理邮箱验证码登录
3. UsernamePasswordAuthenticationFilter: Spring Security默认的用户名密码登录

`认证流程`

1. 用户提交登录请求（密码或验证码）
2. 对应的AuthenticationProvider验证凭据
3. 认证成功后生成JWT token写入Cookie
4. 后续请求通过JwtAuthenticationFilter自动验证
5. 登出时将token加入Redis黑名单

5. 分布式WebSocket实时通信

`核心组件`

- DistributedChatWebSocketHandler: 主要的WebSocket处理器
- ChatSessionRegistryService: Redis-based用户会话注册表
- ChatMessageBroker: Redis pub/sub消息广播器

### 工作机制

1. 连接建立: 用户建立WebSocket连接，注册到Redis会话表
2. 本地会话管理: 每个实例维护本地会话映射localSessions
3. 消息广播:
    - 首先尝试本地广播
    - 如果有用户在其他实例，通过Redis pub/sub跨实例广播
4. 频道订阅: 动态订阅聊天室频道，无用户时自动取消订阅
5. 连接清理: 用户断开时清理本地和Redis中的会话信息

6. 完整工作流程

### 用户登录流程

1. 用户访问前端 → 检查JWT Cookie
2. 无效/过期 → 显示登录页面
3. 用户提交登录 → 后端验证凭据
4. 验证成功 → 生成JWT写入Cookie → 返回用户信息
5. 前端存储用户状态 → 重定向到主页面

### 聊天室创建流程

1. 用户创建聊天室 → 验证JWT权限
2. 保存聊天室到PostgreSQL → 发布ChangeChatroomMemberEvent
3. 返回创建成功 → 前端刷新聊天室列表

### 实时聊天流程

1. 用户进入聊天室 → 建立WebSocket连接
2. 握手拦截器验证权限 → 注册到Redis会话表
3. 广播用户上线消息 → 订阅聊天室频道
4. 用户发送消息 → 保存到MongoDB → 广播给所有在线用户
5. 跨实例广播通过Redis pub/sub实现
6. 用户离开 → 清理会话 → 广播下线消息

### 系统集成流程

PostgreSQL ← JPA → Spring Boot ← WebSocket → Angular Frontend
↑                    ↑
MongoDB ← Messages   Redis ← Sessions/Cache/Pub-Sub
↑                    ↑
RabbitMQ ← Async Events  Email Service


## 微服务重构方案

### 业务边界分析和服务拆分

建议的微服务划分：

🔐 认证授权服务 (auth-service)
- 职责：用户注册、登录、JWT管理、权限验证
- 端口：8081
- 数据库：PostgreSQL (users表)

👥 用户管理服务 (user-service)
- 职责：用户信息管理、用户查询
- 端口：8082
- 数据库：PostgreSQL (users表，与auth-service共享)

🏠 聊天室服务 (chatroom-service)
- 职责：聊天室CRUD、成员管理
- 端口：8083
- 数据库：PostgreSQL (chatrooms表)

💬 消息服务 (message-service)
- 职责：消息存储、历史查询
- 端口：8084
- 数据库：MongoDB

🔔 通知服务 (notification-service)
- 职责：邮件发送、验证码管理
- 端口：8085
- 数据库：Redis (验证码缓存)

⚡ WebSocket网关服务 (websocket-gateway)
- 职责：实时通信、分布式消息广播
- 端口：8086
- 数据库：Redis (会话管理)

🚪 API网关 (api-gateway)
- 职责：路由、负载均衡、统一认证
- 端口：8080

### 数据存储策略

数据库分离方案：

PostgreSQL 分库
auth_db:        users (认证信息)
user_db:        users (用户信息) - 可与auth_db共享
chatroom_db:    chatrooms, chatroom_members

MongoDB
message_db:     chat_messages (按chatroom_id分集合)

Redis
缓存层：       verification_codes, user_sessions
消息队列：     websocket_channels, event_bus
黑名单：       jwt_blacklist

数据一致性策略：

- 最终一致性：通过事件驱动保证跨服务数据同步
- 补偿机制：失败回滚通过Saga模式
- 缓存策略：Redis作为各服务间的数据缓存层

### 服务间通信方案

通信模式设计：

同步通信 (HTTP/REST)
API Gateway → 各微服务    (外部请求路由)
User Service ← Auth Service    (用户信息验证)
Chatroom Service ← User Service    (权限验证)
WebSocket Gateway ← Auth/User/Chatroom Services    (实时验证)

异步通信 (RabbitMQ)
事件总线模式：
- ChatroomCreatedEvent: chatroom-service → message-service
- UserJoinedEvent: chatroom-service → websocket-gateway
- MessageSentEvent: message-service → websocket-gateway
- EmailSendEvent: * → notification-service

技术选型
- 服务发现: Eureka Server (Netflix)
- API网关: Spring Cloud Gateway
- 负载均衡: Ribbon (客户端) + Nginx (服务端)
- 熔断器: Hystrix 或 Resilience4j
- 配置中心: Spring Cloud Config 或 Apollo（已暂时禁用）

### 重构实施步骤 (分阶段进行)

第一阶段：基础设施搭建 (1-2周)

1. 创建服务注册中心 (Eureka Server)
2. 搭建API网关 (Spring Cloud Gateway)
3. 建立配置中心 (Spring Cloud Config) （已暂时禁用）
4. 准备Docker化环境和docker-compose配置

第二阶段：认证服务分离 (1周)

1. 创建auth-service项目
2. 迁移JWT、登录、注册相关代码
3. 修改API网关路由配置
4. 更新前端API调用地址
5. 测试认证功能完整性

第三阶段：用户服务分离 (1周)

1. 创建user-service项目
2. 迁移用户管理相关代码
3. 建立auth-service与user-service的通信
4. 测试用户查询和管理功能

第四阶段：聊天室服务分离 (1-2周)

1. 创建chatroom-service项目
2. 迁移聊天室管理代码
3. 实现与user-service的服务调用
4. 建立事件发布机制 (RabbitMQ)
5. 测试聊天室创建、修改、删除功能

第五阶段：消息服务分离 (1周)

1. 创建message-service项目
2. 迁移消息存储和查询功能
3. 监听聊天室事件，建立消息关联
4. 测试消息历史查询功能

第六阶段：WebSocket网关重构 (1-2周)

1. 创建websocket-gateway项目
2. 重构分布式WebSocket处理逻辑
3. 建立与各服务的通信机制
4. 实现跨服务的实时消息广播
5. 压力测试实时通信功能

第七阶段：通知服务分离 (1周)

1. 创建notification-service项目
2. 迁移邮件发送功能
3. 实现异步邮件处理
4. 测试验证码和重置密码功能

第八阶段：系统优化和完善 (1-2周)

1. 添加熔断器和重试机制
2. 完善监控和日志
3. 性能测试和调优
4. 更新部署脚本和CI/CD

### 项目配置和部署方案

新的项目结构：

SimpleChatApp_Microservices/
ChatApp_BackEnd_Microservices/
auth-service/
user-service/
chatroom-service/
message-service/
notification-service/
websocket-gateway/
eureka-server/
ChatApp_FrontEnd/


### 重构关键要点和建议

🎯 核心原则：

1. 渐进式重构：不要一次性拆分所有服务，按阶段进行
2. 保持功能完整：每个阶段都要确保系统功能正常
3. 数据一致性：通过事件驱动和补偿机制保证数据同步
4. 向后兼容：API变更要保持向后兼容性

⚠️ 注意事项：

1. 分布式事务：避免跨服务事务，采用最终一致性
2. 服务依赖：防止循环依赖，建立清晰的依赖关系
3. 错误处理：添加熔断、重试、降级机制
4. 数据冗余：适度冗余以减少跨服务调用

🚀 学习收益：

- 理解微服务架构设计原则
- 掌握Spring Cloud生态使用
- 学习分布式系统的挑战和解决方案
- 实践DevOps和容器化部署

📚 技术栈更新：

原架构：Spring Boot + PostgreSQL + MongoDB + Redis + RabbitMQ
新架构：Spring Cloud + Eureka + Gateway + Config + 原数据栈

`第二阶段代码重构已完成，容器运行成功，尚未进行api-gateway的auth-service相关路由测试`