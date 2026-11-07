# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖(利用 Docker 缓存层)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建上传目录和日志目录
RUN mkdir -p /app/uploads/song /app/uploads/songListPic /app/uploads/singerPic /app/uploads/userImages \
    && mkdir -p /app/logs

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/Music-backend-1.1.0.jar app.jar

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
