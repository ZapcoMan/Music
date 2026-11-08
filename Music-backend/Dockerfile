# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖(利用 Docker 缓存层)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
# 产物名带版本号，运行阶段不能再硬编码它（升版即 COPY 源不存在、构建直接断）。
# 这里顺手挑掉 Spring Boot repackage 留下的 .original 空壳，归一成稳定名字 app.jar。
COPY src ./src
RUN mvn clean package -DskipTests -B \
 && JAR="$(ls /app/target/*.jar | grep -v '\.original$' | head -1)" \
 && cp "$JAR" /app/app.jar

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建上传目录和日志目录，并交给非 root 用户持有。
# 基础镜像默认以 root 运行，容器一旦被突破，进程即容器内 root。
RUN mkdir -p /app/uploads/song /app/uploads/songListPic /app/uploads/singerPic /app/uploads/userImages \
    && mkdir -p /app/logs \
    && addgroup -S app && adduser -S app -G app \
    && chown -R app:app /app

# 从构建阶段复制 jar 包（产物名已在 builder 里归一，不再耦合版本号）
COPY --from=builder /app/app.jar app.jar

# 以非 root 运行
USER app

# 暴露端口
EXPOSE 9171

# 设置时区
ENV TZ=Asia/Shanghai

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
  CMD curl -f http://localhost:9171/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Xms256m", \
  "-Xmx512m", \
  "-jar", "app.jar"]
