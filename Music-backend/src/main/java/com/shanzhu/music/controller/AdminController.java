package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Admin;
import com.shanzhu.music.entity.vo.AdminLoginVo;
import com.shanzhu.music.service.AdminService;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import com.shanzhu.music.service.TokenBlacklistService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;


/**
 * 后台管理 控制层
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员登录和管理相关接口")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtils jwtUtils;
    private final IpUtils ipUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final OperationLogService operationLogService;

    /**
     * 管理员退出登录：吊销当前令牌
     *
     * @param request HttpServletRequest
     * @return 结果
     */
    @Operation(summary = "管理员登出", description = "使当前管理员令牌立即失效")
    @DeleteMapping(value = "/api/v1/admin/sessions")
    @RequireUserType("admin")
    public R<Void> logoutAdmin(HttpServletRequest request,
                         @RequestAttribute("userId") Integer userId,
                         @RequestAttribute("username") String username,
                         @RequestAttribute("userType") String userType) {
        // 与鉴权拦截器同一口径：只认头，不认 URL 参数，
        // 否则完整令牌会随 query string 进访问日志留档
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return R.error("缺少令牌");
        }
        // 本地会话一定清掉了；但若 Redis 不可用，服务端没能把它拉黑，
        // 就必须如实说明——否则拿着泄漏令牌的人以为对方已经安全退出
        boolean revoked = tokenBlacklistService.revoke(token);
        operationLogService.logLogout(userId, username, userType, ipUtils.getIpAddress(request));
        return R.ok(revoked ? "已退出登录"
                : "已退出登录，但服务端暂时无法吊销该令牌，它在自然过期前仍可能有效");
    }

    /**
     * 管理员登录(JWT + IP绑定)
     *
     * @param name     账号名
     * @param password 密码
     * @param request  HttpServletRequest
     * @return 登录结果和Token
     */
    @Operation(summary = "管理员登录", description = "管理员使用账号密码登录,返回JWT Token(IP绑定)")
    @PostMapping(value = "/api/v1/admin/sessions")
    @RateLimit(key = "login:admin:", timeWindow = 60, maxRequests = 5, message = "登录尝试次数过多，请60秒后再试")
    public R<AdminLoginVo> loginStatus(
            // 不在 @Parameter 里给 example：仓库公开后 Swagger UI 也是公开的，
            // 预填的 admin/123456 等于把默认账号与一个弱口令候选一起摊给扫描者
            @Parameter(description = "管理员账号", required = true) @RequestParam String name,
            @Parameter(description = "管理员密码", required = true) @RequestParam String password,
            HttpServletRequest request) {
        log.info("管理员登录请求 - username={}", name);
        
        //获取客户端IP地址
        String ip = ipUtils.getIpAddress(request);

        //验证账号密码
        if (!adminService.verifyPassword(name, password)) {
            log.warn("管理员登录失败 - username={}, ip={}", name, ip);
            operationLogService.logLogin(null, name, "admin", ip, false);
            return R.error("用户名或密码错误");
        }

        //获取管理员信息
        Admin admin = adminService.getByName(name);
        if (admin == null) {
            return R.error("管理员不存在");
        }

        //生成JWT Token(包含用户ID、用户名、用户类型、IP地址)
        String token = jwtUtils.generateToken(admin.getId(), name, "admin", ip);

        //构建返回结果
        AdminLoginVo result = new AdminLoginVo();
        result.setName(name);
        result.setToken(token);
        // 与 jwt.expiration 同源，不再各写一份
        result.setExpiresIn(jwtUtils.getExpirationSeconds());

        operationLogService.logLogin(admin.getId(), name, "admin", ip, true);
        log.info("管理员登录成功 - adminId={}, username={}, ip={}", admin.getId(), name, ip);
        return R.ok("登录成功", result);
    }

    /**
     * 打包项目接口
     *
     * @return modelAndView
     */
    @Operation(summary = "管理页面", description = "返回后台管理页面")
    @GetMapping(value = "/manage")
    public ModelAndView index() {
        return new ModelAndView("/src/main/resources/static/screenIndex/index.html");
    }

}
