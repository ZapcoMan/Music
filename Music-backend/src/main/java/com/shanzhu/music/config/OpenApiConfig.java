package com.shanzhu.music.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类
 */
@Configuration
public class OpenApiConfig {

    /**
     * 文档版本与 actuator /info 共用 info.app.version 这一真源。
     * 此前这里写死字面值，发布时必须记得和 pom、Dockerfile、application.yml 一起手改，
     * 漏掉一处 Swagger UI 就对外显示旧版本——而它恰恰是最不容易想起来的一处。
     */
    @Value("${info.app.version}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("音乐网站 API 文档")
                        .description("音乐网站后台管理系统 RESTful API 接口文档")
                        .version(appVersion)
                        .contact(new Contact()
                                .name("ShanZhu")
                                .email("shanzhu@example.com")
                                .url("https://github.com/shanzhu"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
