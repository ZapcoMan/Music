package com.shanzhu.music.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Actuator自定义信息贡献者
 */
@Component
public class AppInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", "Music Backend System")
                .withDetail("description", "音乐网站后台管理系统")
                .withDetail("version", "1.0.0")
                .withDetail("javaVersion", System.getProperty("java.version"))
                .withDetail("osName", System.getProperty("os.name"))
                .withDetail("startTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .withDetail("features", new String[]{
                    "JWT认证 + IP绑定",
                    "API限流",
                    "Redis缓存",
                    "BCrypt密码加密",
                    "数据库迁移(Flyway)",
                    "分页查询",
                    "协同过滤推荐"
                });
    }
}
