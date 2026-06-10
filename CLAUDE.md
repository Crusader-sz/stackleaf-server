# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# 编译 (需要先启动 Docker 服务)
docker compose up -d
export $(cat .env | grep -v '^#' | xargs)
mvn compile

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=ConnectionTest

# 启动应用 (dev 环境)
mvn spring-boot:run

# Docker 环境部署
SPRING_PROFILES_ACTIVE=docker mvn spring-boot:run
```

环境变量通过 `.env` 文件管理，由 `spring-dotenv` 库自动加载，无需手动 export（但 mvn 命令行仍需手动 export）。

## Tech Stack

- Java 17, Spring Boot 3.5.14, MyBatis-Plus 3.5.15, Sa-Token 1.45.0
- MySQL 8.0, Redis 7, MinIO (对象存储)
- Knife4j (OpenAPI3 接口文档, 路径 /doc.html)
- QQ 邮箱 SMTP (spring-boot-starter-mail)

## Architecture

标准分层架构，基础包 `com.crusader.stackleafserver`:

```
controller/     → REST 接口，返回 Result<T>
service/        → 业务接口
service/impl/   → 业务实现，继承 ServiceImpl<Mapper, Entity>
mapper/         → MyBatis-Plus BaseMapper 接口
model/entity/   → 数据库实体 (@TableName)
model/dto/      → 请求对象 (@Valid 校验)
model/vo/       → 响应视图对象 (脱敏，无密码/邮箱)
config/         → Spring 配置类
constant/       → 常量类 (MessageConstant, ResultCodeConstant, VerificationConstant)
enumeration/    → 枚举类 (UserRole, UserStatus)
exception/      → 自定义异常 (BusinessException)
handler/        → 全局异常处理器 (GlobalExceptionHandler)
interceptor/    → Sa-Token 登录拦截器
```

### 关键设计决策

- **认证**: Sa-Token，拦截器 `LoginInterceptor` 注册在 `WebConfig`，公开接口通过 `excludePathPatterns` 放行（使用 Ant 通配符 `*`/`**`，不支持 `{id}` 占位符）
- **异常处理**: 业务异常统一抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 捕获返回 `Result.error(code, message)`；禁止直接抛 RuntimeException
- **消息管理**: 所有用户可见的提示文本集中在 `MessageConstant`，禁止硬编码
- **错误码**: `ResultCodeConstant` 定义 HTTP 语义码 (200/400/401/403/404/409/429/500)
- **计数器更新**: 使用 `LambdaUpdateWrapper.setSql("count = count + 1")` 原子操作，扣减时用 `GREATEST(count - 1, 0)` 防负数
- **密码**: BCrypt 加密，`UserVO` 不包含 password 和 email 字段
- **配置**: 敏感值通过 `.env` 环境变量注入，`application.yaml` 不含硬编码密码；dev/docker 环境通过 Spring Profile 切换

### 数据库

9 张表，SQL 在 `docker/mysql/init/schema.sql`，Docker 启动时自动执行:
- `user`, `category`, `tag`, `article`, `article_tag`, `comment`, `article_like`, `user_follow`, `article_favorite`
- 不加物理外键，通过逻辑外键列 + 索引关联
- 关注/点赞/收藏使用 UNIQUE 约束防重复，插入时 catch `DuplicateKeyException`

### Docker 服务

`docker-compose.yml` 定义: MySQL (3306), Redis (6379), MinIO (9000/9001)
- MySQL 初始化脚本: `docker/mysql/init/schema.sql`
- `.env` 文件已在 `.gitignore` 中排除

## Conventions

- 阿里巴巴 Java 开发规范
- 实体类用 `@Data` + `@TableName`，ID 用 `@TableId(type = IdType.AUTO)`
- Mapper 继承 `BaseMapper<T>`，Service 继承 `ServiceImpl<M, T>`
- Controller 方法直接调 Service，不写额外业务逻辑
- `BeanUtils.copyProperties()` 用于 Entity → VO 转换
- 事务注解: 写操作加 `@Transactional(rollbackFor = Exception.class)`
