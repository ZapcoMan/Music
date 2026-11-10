package com.shanzhu.music.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类
 */
@Configuration
public class OpenApiConfig {

    /**
     * 版本读 build-info，不再读 info.app.version。
     *
     * 原先这里 @Value("${info.app.version}")，而 AppInfoContributor 读 build-info
     * （唯一出处是 pom）——两条路各说各话：升版时改 pom 忘改 application.yml，
     * Swagger UI 显示旧版本、/actuator/info 的 app 明细又取决于两个 InfoContributor
     * 谁后写（同名 key "app" 互相覆盖），表现是"版本时对时错"而不是报错。
     * 现在文档版本、actuator 版本、jar 名同源于 pom 一处。
     */
    private final ObjectProvider<BuildProperties> buildProperties;

    public OpenApiConfig(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        String author = "zapcoman";
        String authorEmail = "zapcoman@zapcoman.com";
        String authorUrl = "https://github.com/ZapcoMan";
        BuildProperties props = buildProperties.getIfAvailable();
        return new OpenAPI()
                .info(new Info()
                        .title("音乐网站 API 文档")
                        .description("音乐网站后台管理系统 RESTful API 接口文档")
                        .version(props != null ? props.getVersion() : "未知（构建未生成 build-info）")
                        .contact(new Contact()
                                .name(author)
                                .email(authorEmail)
                                .url(authorUrl))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
