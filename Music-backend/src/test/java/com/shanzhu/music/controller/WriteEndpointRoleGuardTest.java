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
 * 写接口角色声明的守卫。
 *
 * 拦截器的 fail-closed 只能在运行时拒绝"没声明角色的写接口"；这个测试让同一件事在开发期
 * 就变红，不必等到有人真的调那个接口。
 *
 * 之所以需要它：本轮补 @RequireUserType 时，22 处标注里有 21 处长期没有任何用例覆盖，
 * 漏标注的后果不是报错而是静默放行——这正是 /song 那个洞能存在许久的机制性原因。
 * 有了这条守卫，新增写接口忘标角色会立刻让本用例失败。
 */
class WriteEndpointRoleGuardTest extends BaseTest {

    /**
     * 会改变服务端状态的动词，与 JwtAuthenticationInterceptor 的口径保持一致。
     */
    private static final Set<RequestMethod> WRITE_METHODS =
            Set.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    /**
     * 免登录写接口：由 WebMvcConfig 白名单排除在拦截器之外，因此天然不需要角色声明。
     * 这里必须显式列出，否则守卫会把它们报成违规；也顺便提醒：往里加接口等于开一个匿名写入口。
     */
    private static final Set<String> LOGIN_FREE_WRITE_ENDPOINTS = Set.of(
            "/admin/login/status",
            "/consumer/login",
            "/consumer/add");

    /**
     * 按 bean 名注入：Actuator 也会注册一个同类型的 controllerEndpointHandlerMapping，
     * 不指定就会因"找到 2 个候选"而装配失败。
     */
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("所有写接口必须显式声明可访问角色")
    void everyWriteEndpointDeclaresRequiredUserRole() {
        List<String> missing = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            // 只审计本项目自己的 Controller：Spring Boot 的 BasicErrorController 也挂在 /error
            // 上且带写方法，它是框架错误渲染入口、由白名单排除鉴权，不该被算成漏标注
            if (!handler.getBeanType().getName().startsWith("com.shanzhu.music.")) {
                continue;
            }
            if (!isWriteEndpoint(entry.getKey())) {
                continue;
            }
            String patterns = entry.getKey().getPatternValues().toString();
            if (coversLoginFreeEndpoint(entry.getKey())) {
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
                () -> "以下写接口未声明 @RequireUserType，运行时会被 fail-closed 直接 403，"
                        + "请补上角色域（管理接口标 admin、用户自助接口标 consumer）：\n  - "
                        + String.join("\n  - ", missing));
    }

    /**
     * 未限定动词的 @RequestMapping 也算写接口：它同时接受 POST/DELETE，
     * 若放过就等于留了一个漏标注的口子。
     */
    private boolean isWriteEndpoint(RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        return methods.isEmpty() || methods.stream().anyMatch(WRITE_METHODS::contains);
    }

    private boolean coversLoginFreeEndpoint(RequestMappingInfo info) {
        return info.getPatternValues().stream().anyMatch(LOGIN_FREE_WRITE_ENDPOINTS::contains);
    }
}
