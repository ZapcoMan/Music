package com.shanzhu.music.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Actuator 自定义信息贡献者。
 *
 * 版本从 build-info.properties 读，不再写死字符串：此前这里写 "1.0.0"，
 * 而 pom 是 2.0.1、application.yml 的 info.app.version 也是 2.0.1，
 * 三处各说各话，且改版本号时这里不会报错、只会一直骗人。
 */
@Component
@RequiredArgsConstructor
public class AppInfoContributor implements InfoContributor {

    /**
     * 由 spring-boot-maven-plugin 的 build-info goal 生成（绑定 generate-resources，
     * 所以 mvn test 与 mvn package 都会有）。
     * 用 ObjectProvider 而不是直接注入，是为了 IDE 里直接运行主类这种不走 Maven
     * 生命周期的场景：那时该文件不存在，硬依赖会让应用启动当场失败，
     * 而版本信息属于"缺了也只是缺了"的诊断数据，不该阻塞启动。
     */
    private final ObjectProvider<BuildProperties> buildProperties;

    /**
     * 本 bean 的创建时刻近似启动时刻。
     * 此前这里每次调用都取 LocalDateTime.now()，字段名却叫 startTime ——
     * 每访问一次 /info 就"启动"一次，排查启动时间时是一个明确的误导。
     */
    private final String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @Override
    public void contribute(Info.Builder builder) {
        BuildProperties props = buildProperties.getIfAvailable();
        builder.withDetail("app", "Music Backend System")
                .withDetail("description", "音乐网站后台管理系统")
                .withDetail("version", props != null ? props.getVersion() : "未知（构建未生成 build-info）")
                .withDetail("javaVersion", System.getProperty("java.version"))
                .withDetail("osName", System.getProperty("os.name"))
                .withDetail("startTime", startTime)
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
