# 构建阶段
# 原先是 node:16-alpine，该版本 2023-09 已 EOL、不再有安全补丁。
# 升到 20 后本地实测 webpack 3 的 md4 依赖并未触发 OpenSSL 3 的
# unsupported 报错（node 24 下 npm run build 完整通过），
# 因此不需要额外挂 --openssl-legacy-provider。
FROM node:20-alpine AS builder

WORKDIR /app

# 复制 package.json 并安装依赖
# 用 npm ci 而不是 npm install：仓库里有 package-lock.json，
# install 会按 semver 重新解析、构建不可复现，同一份代码两次构建产物可能不同。
COPY package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# 复制源代码并构建
COPY . .

# 设置 API 基础 URL (可以通过 build arg 传入)
ARG VUE_APP_API_BASE_URL=http://localhost:9171
ENV VUE_APP_API_BASE_URL=$VUE_APP_API_BASE_URL

RUN npm run build

# 生产阶段 - 使用 Nginx
FROM nginx:alpine

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制静态文件到 Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
