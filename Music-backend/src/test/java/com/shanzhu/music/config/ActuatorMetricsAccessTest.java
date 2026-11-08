package com.shanzhu.music.config;

import com.shanzhu.music.BaseTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 指标管线是否真的存在。
 *
 * prod 配置长期把 prometheus 列在 exposure.include 里，但依赖从来没加过，
 * 于是配置看起来"监控已就绪"、实际一根指标都抓不到。
 * 这个类就是让那种状态无法维持。
 *
 * @AutoConfigureObservability 不是可选项：@SpringBootTest 默认会挂上
 * DisableObservabilityContextCustomizer，把 management.defaults.metrics.export.enabled
 * 置为 false，任何 *MetricsExportAutoConfiguration 都因此不装配。
 * 不加它，本类的两条断言会在"依赖缺失"与"测试框架主动关闭"之间无法区分——
 * 我正是先踩了这个坑，才把 404 误判成鉴权拦截。
 */
@AutoConfigureObservability
class ActuatorMetricsAccessTest extends BaseTest {

    /**
     * 用 ObjectProvider 而不是直接注入：条件不满足时 bean 就是不存在的，
     * 直接注入会让整个测试上下文启动失败，
     * 报错信息停在"bean 找不到"上，看不出真正缺的是依赖还是暴露配置。
     */
    @Autowired
    private ObjectProvider<PrometheusScrapeEndpoint> scrapeEndpoint;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * 这里刻意不发 HTTP 请求。
     *
     * 最初写的是 MockMvc GET /actuator/prometheus 断 200，结果恒为 404——
     * MockMvcBuilders.webAppContextSetup 只接管 MVC 的 RequestMappingHandlerMapping，
     * 而 actuator 端点注册在独立的 ControllerEndpointHandlerMapping 上，
     * MockMvc 根本看不见它。那条断言在任何配置下都不会变绿，
     * 属于"看起来在测、其实什么都测不到"的用例。
     * 断 bean 是否被装配，才对应"依赖 + 暴露配置"这两个真实开关。
     * 端到端的抓取可达性需要真端口（RANDOM_PORT + HTTP）才能覆盖，另行验证。
     */
    @Test
    @DisplayName("Prometheus 抓取端点已装配")
    void scrapeEndpointIsConfigured() {
        assertNotNull(scrapeEndpoint.getIfAvailable(),
                "为 null 说明要么缺 micrometer-registry-prometheus，"
                        + "要么当前 profile 的 exposure 没把 prometheus 列进来");
    }

    @Test
    @DisplayName("指标后端是 Prometheus 注册表")
    void registryIsPrometheus() {
        // 没有 registry 时 Boot 会退化成 SimpleMeterRegistry：
        // 指标能记，但 /actuator/prometheus 就算可达也吐不出 Prometheus 文本格式
        assertInstanceOf(PrometheusMeterRegistry.class, meterRegistry,
                "注册表类型不对，抓取端拿不到可解析的指标文本");
    }
}
