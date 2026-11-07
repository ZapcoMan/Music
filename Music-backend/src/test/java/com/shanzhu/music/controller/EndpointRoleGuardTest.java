package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import com.shanzhu.music.annotation.RequireUserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务端点角色声明的守卫。
 *
 * 拦截器的 fail-closed 只能在运行时拒绝"没声明角色的端点"；这个测试让同一件事在开发期
 * 就变红，不必等到有人真的调那个接口。
 *
 * 之所以需要它：本轮补 @RequireUserType 时发现，已有标注里绝大多数长期没有任何用例覆盖，
 * 漏标注的后果不是报错而是静默放行——这正是 /song 那个洞能存在许久的机制性原因。
 * 有了这条守卫，新增端点忘标角色会立刻让本用例失败。
 *
 * 覆盖范围与拦截器同口径：读写都查。只查写接口的话，漏标的读接口照样能把
 * 别人的数据返回给已登录的任意角色，而守卫和运行时都沉默。
 */
class EndpointRoleGuardTest extends BaseTest {

    /**
     * 由 WebMvcConfig 的 excludePathPatterns 排除在拦截器之外的路径，因此不需要角色声明。
     * 必须逐条列全——往这个清单里加一项，等于开一个匿名入口。
     */
    private static final Set<String> EXCLUDED_PATHS = Set.of("/manage");

    /**
     * 由 JwtAuthenticationInterceptor#ANONYMOUS_ENTRIES 按 (方法, 路径) 放行的入口。
     *
     * 与上面那份的区别在于它有方法维度：POST /consumers/sessions 是登录（匿名可调），
     * DELETE /consumers/sessions 是登出（必须带令牌并声明角色）。同一路径字面下
     * 只有全部方法都在这份集合里，才算真正的匿名端点。
     */
    private static final Set<String> ANONYMOUS_ENTRIES = Set.of(
            "POST /api/v1/admin/sessions",
            "POST /api/v1/consumers/sessions",
            "POST /api/v1/consumers/registrations");

    /**
     * 按 bean 名注入：Actuator 也会注册一个同类型的 controllerEndpointHandlerMapping，
     * 不指定就会因"找到 2 个候选"而装配失败。
     */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("所有业务端点必须显式声明可访问角色")
    void everyEndpointDeclaresRequiredUserRole() {
        List<String> missing = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            // 只审计本项目自己的 Controller：Spring Boot 的 BasicErrorController 也挂在 /error
            // 上且带写方法，它是框架错误渲染入口、由白名单排除鉴权，不该被算成漏标注
            if (!handler.getBeanType().getName().startsWith("com.shanzhu.music.")) {
                continue;
            }
            String patterns = entry.getKey().getPatternValues().toString();
            if (isAnonymousEndpoint(entry.getKey())) {
                continue;
            }
            RequireUserType declared = AnnotatedElementUtils.findMergedAnnotation(
                    handler.getMethod(), RequireUserType.class);
            if (declared == null) {
                declared = AnnotatedElementUtils.findMergedAnnotation(
                        handler.getBeanType(), RequireUserType.class);
            }
            if (declared == null) {
                missing.add(handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName()
                        + " " + patterns);
            }
        }

        assertTrue(missing.isEmpty(),
                () -> "以下端点未声明 @RequireUserType，运行时会被 fail-closed 直接 403，"
                        + "请补上角色域（管理接口标 admin、用户自助接口标 consumer、"
                        + "两类令牌都要能用的标 {consumer, admin}）：\n  - "
                        + String.join("\n  - ", missing));
    }

    private boolean isAnonymousEndpoint(RequestMappingInfo info) {
        Set<String> patterns = info.getPatternValues();
        // 通道一：WebMvcConfig 的 excludePathPatterns 只看路径，整个端点都不进拦截器
        if (EXCLUDED_PATHS.containsAll(patterns)) {
            return true;
        }
        // 通道二：拦截器按 (方法, 路径) 放行。未限定方法意味着该 mapping 接受任意动词，
        // 那样连同一字面上的匿名方法也一起被放过，不算真正的匿名端点
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            return false;
        }
        return patterns.stream().allMatch(pattern -> methods.stream()
                .allMatch(method -> ANONYMOUS_ENTRIES.contains(method.name() + " " + pattern)));
    }
}
