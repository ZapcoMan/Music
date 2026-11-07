package com.shanzhu.music.controller;

import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.po.Admin;
import com.shanzhu.music.service.AdminService;
import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import com.shanzhu.music.service.TokenBlacklistService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 管理员退出登录：吊销当前令牌
     *
     * @param request HttpServletRequest
     * @return 结果
     */
    @Operation(summary = "管理员登出", description = "使当前管理员令牌立即失效")
    @PostMapping(value = "/admin/logout")
    public R logoutAdmin(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            return R.error("缺少令牌");
        }
        tokenBlacklistService.revoke(token);
        return R.ok("已退出登录");
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
    @RequestMapping(value = "/admin/login/status", method = RequestMethod.POST)
    @RateLimit(key = "login:admin:", timeWindow = 60, maxRequests = 5, message = "登录尝试次数过多，请60秒后再试")
    public R<Map<String, Object>> loginStatus(
            @Parameter(description = "管理员账号", required = true, example = "admin") @RequestParam String name,
            @Parameter(description = "管理员密码", required = true, example = "123456") @RequestParam String password,
            HttpServletRequest request) {
        log.info("管理员登录请求 - username={}", name);
        
        //获取客户端IP地址
        String ip = ipUtils.getIpAddress(request);

        //验证账号密码
        if (!adminService.verifyPassword(name, password)) {
            log.warn("管理员登录失败 - username={}, ip={}", name, ip);
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
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("token", token);
        result.put("expiresIn", jwtUtils.getExpirationSeconds()); // 与 jwt.expiration 同源，不再各写一份

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
