#!/bin/bash

# 音乐网站 Docker 部署脚本
# 用法: ./deploy.sh [start|stop|restart|build|logs|clean]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}  音乐网站 Docker 部署脚本${NC}"
echo -e "${GREEN}================================${NC}"

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装${NC}"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: Docker Compose 未安装${NC}"
    exit 1
fi

# 确定使用的 docker-compose 命令
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

case "$1" in
    start)
        echo -e "${YELLOW}启动所有服务...${NC}"
        cd "$PROJECT_ROOT"
        $DOCKER_COMPOSE up -d
        echo -e "${GREEN}✓ 服务启动成功${NC}"
        echo -e "${YELLOW}等待服务就绪...${NC}"
        sleep 10
        echo -e "${GREEN}================================${NC}"
        echo -e "${GREEN}访问地址:${NC}"
        echo -e "  管理端: http://localhost:8080"
        echo -e "  用户端: http://localhost:8081"
        echo -e "  后端API: http://localhost:9171"
        echo -e "  Swagger UI: http://localhost:9171/swagger-ui.html"
        echo -e "${GREEN}================================${NC}"
        ;;
    
    stop)
        echo -e "${YELLOW}停止所有服务...${NC}"
        cd "$PROJECT_ROOT"
        $DOCKER_COMPOSE down
        echo -e "${GREEN}✓ 服务已停止${NC}"
        ;;
    
    restart)
        echo -e "${YELLOW}重启所有服务...${NC}"
        cd "$PROJECT_ROOT"
        $DOCKER_COMPOSE down
        $DOCKER_COMPOSE up -d
        echo -e "${GREEN}✓ 服务已重启${NC}"
        ;;
    
    build)
        echo -e "${YELLOW}重新构建所有镜像...${NC}"
        cd "$PROJECT_ROOT"
        $DOCKER_COMPOSE build --no-cache
        echo -e "${GREEN}✓ 镜像构建完成${NC}"
        ;;
    
    logs)
        echo -e "${YELLOW}查看服务日志 (Ctrl+C 退出)...${NC}"
        cd "$PROJECT_ROOT"
        if [ -n "$2" ]; then
            $DOCKER_COMPOSE logs -f "$2"
        else
            $DOCKER_COMPOSE logs -f
        fi
        ;;
    
    clean)
        echo -e "${RED}警告: 这将删除所有容器、卷和镜像!${NC}"
        read -p "确认继续? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            cd "$PROJECT_ROOT"
            $DOCKER_COMPOSE down -v --rmi all
            echo -e "${GREEN}✓ 清理完成${NC}"
        else
            echo -e "${YELLOW}操作已取消${NC}"
        fi
        ;;
    
    status)
        echo -e "${YELLOW}服务状态:${NC}"
        cd "$PROJECT_ROOT"
        $DOCKER_COMPOSE ps
        ;;
    
    *)
        echo -e "${YELLOW}用法: $0 {start|stop|restart|build|logs|clean|status}${NC}"
        echo ""
        echo "命令说明:"
        echo "  start   - 启动所有服务"
        echo "  stop    - 停止所有服务"
        echo "  restart - 重启所有服务"
        echo "  build   - 重新构建所有镜像"
        echo "  logs    - 查看服务日志 (可指定服务名)"
        echo "  clean   - 清理所有容器、卷和镜像"
        echo "  status  - 查看服务状态"
        exit 1
        ;;
esac

exit 0
