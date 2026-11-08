package com.shanzhu.music.controller;

import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.ConsumerProfileRequest;
import com.shanzhu.music.entity.dto.ConsumerRequest;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.entity.vo.ConsumerLoginVo;
import com.shanzhu.music.entity.vo.ConsumerPublicVo;
import com.shanzhu.music.entity.vo.UploadConsumerPicVo;
import com.shanzhu.music.service.ConsumerService;
import com.shanzhu.music.service.OperationLogService;
import com.shanzhu.music.service.TokenBlacklistService;
import com.shanzhu.music.utils.IpUtils;
import com.shanzhu.music.utils.JwtUtils;
import com.shanzhu.music.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 前台用户相关 控制层
 *
 * @author: ShanZhu
 * @date: 2023-11-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumers")
@RequireUserType({"consumer", "admin"})
@RequiredArgsConstructor
@Tag(name = "前台用户管理", description = "前台用户注册、登录、信息查询等接口")
public class ConsumerController {

    private final ConsumerService consumerService;
    private final JwtUtils jwtUtils;
    private final IpUtils ipUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final FileStorageService fileStorageService;
    private final OperationLogService operationLogService;

    /**
     * 退出登录：吊销当前令牌，使其在有效期内立即不可用。
     * 此前只由前端删除本地 token，泄漏出去的令牌在服务端仍然有效。
     *
     * @param request HttpServletRequest
     * @return 结果
     */
    @Operation(summary = "用户登出", description = "使当前令牌立即失效")
    @DeleteMapping(value = "/sessions")
    @RequireUserType({"consumer", "admin"})
    public R<Void> logout(HttpServletRequest request,
                    @RequestAttribute("userId") Integer userId,
                    @RequestAttribute("username") String username,
                    @RequestAttribute("userType") String userType) {
        // 与鉴权拦截器同一口径：只认头，不认 URL 参数，
        // 否则完整令牌会随 query string 进访问日志留档
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return R.error("缺少令牌");
        }
        tokenBlacklistService.revoke(token);
        operationLogService.logLogout(userId, username, userType, ipUtils.getIpAddress(request));
        return R.ok("已退出登录");
    }

    /**
     * 添加前台用户
     *
     * @param request 用户信息
     * @return 添加结果
     */
    @Operation(summary = "注册用户", description = "注册新的前台用户账号")
    @RateLimit(key = "register:consumer:", timeWindow = 60, maxRequests = 5, message = "注册过于频繁，请60秒后再试")
    @PostMapping(value = "/registrations")
    public R<Void> addConsumer(@Valid @RequestBody ConsumerRequest request, HttpServletRequest httpRequest) {
        // 查重、生日转换、DTO→PO 组装与密码加密都在 service，
        // 注册成功即返回；用户名已存在/写入失败由 service 抛 BusinessException 走全局异常处理
        Consumer consumer = consumerService.register(request);
        operationLogService.logAdd(consumer.getId(), request.getUsername(), "consumer",
                "用户模块", "注册新用户", ipUtils.getIpAddress(httpRequest));
        return R.ok("添加成功");
    }

    /**
     * 修改前台用户
     *
     * @param request 用户信息
     * @return 修改结果
     */
    @RateLimit(key = "update:consumer:", timeWindow = 60, maxRequests = 10, message = "资料修改过于频繁，请稍后再试")
    @PatchMapping(value = "/profile")
    @RequireUserType("consumer")
    public R<Void> updateConsumer(@RequestAttribute("userId") Integer currentUserId,
                            @RequestAttribute("username") String currentUsername,
                            @RequestAttribute("userType") String currentUserType,
                            @Valid @RequestBody ConsumerProfileRequest request,
                            HttpServletRequest httpRequest) {
        // 身份只取令牌，生日转换与组装在 service；"用户不存在"由 service 抛 BusinessException
        if (consumerService.updateProfile(request, currentUserId)) {
            operationLogService.logUpdate(currentUserId, currentUsername, currentUserType,
                    "用户模块", "修改个人资料", ipUtils.getIpAddress(httpRequest));
            return R.ok("修改成功");
        }
        return R.error("修改失败");
    }

    /**
     * 删除前台用户
     *
     * @param id 用户id
     * @return 删除结果
     */
    /**
     * 管理员修改指定用户的资料
     *
     * 与自助的 PATCH /profile 共用同一个 service，差别只在身份来源：目标用户取自
     * 路径，且只有 admin 能打（方法级 @RequireUserType 覆盖类级）。
     *
     * 之所以要单开一个端点：管理端"编辑用户"此前一直打的是自助端点，那里
     * @RequireUserType("consumer") 把 admin 令牌挡在门外，请求必然 403；而该端点
     * 的身份又只取自令牌、不认参数，本来就无法指向别人。两条路都必须分开。
     *
     * @param id      目标用户id
     * @param request 资料请求
     * @return 修改结果
     */
    @Operation(summary = "管理员修改用户资料", description = "按用户id修改资料；用户名不可改动，口令留空即保持不变")
    @RateLimit(key = "update:consumer:admin:", timeWindow = 60, maxRequests = 10, message = "修改过于频繁，请60秒后再试")
    @PatchMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Void> updateConsumerByAdmin(@RequestAttribute("userId") Integer operatorId,
                                         @RequestAttribute("username") String operatorName,
                                         @RequestAttribute("userType") String operatorType,
                                         @Parameter(description = "用户ID", required = true, example = "1")
                                         @PathVariable("id") Integer id,
                                         @Valid @RequestBody ConsumerProfileRequest request,
                                         HttpServletRequest httpRequest) {
        // 目标 id 来自路径而非令牌；用户名仍由 service 沿用库里现值，改不动
        if (consumerService.updateProfile(request, id)) {
            operationLogService.logUpdate(operatorId, operatorName, operatorType,
                    "用户模块", "管理员修改用户资料 id=" + id, ipUtils.getIpAddress(httpRequest));
            return R.ok("修改成功");
        }
        return R.error("修改失败");
    }

    @RateLimit(key = "delete:consumer:", timeWindow = 60, maxRequests = 10, message = "用户删除过于频繁，请稍后再试")
    @DeleteMapping(value = "/{id}")
    @RequireUserType("admin")
    public R<Boolean> deleteConsumer(@RequestAttribute("userId") Integer operatorId,
                                  @RequestAttribute("username") String operatorName,
                                  @RequestAttribute("userType") String operatorType,
                                  @PathVariable("id") Integer id, HttpServletRequest httpRequest) {
        boolean deleted = consumerService.delete(id);
        if (deleted) {
            operationLogService.logDelete(operatorId, operatorName, operatorType,
                    "用户模块", "删除前台用户 id=" + id, ipUtils.getIpAddress(httpRequest));
        }
        return R.ok(deleted);
    }

    /**
     * 查询用户公开信息
     *
     * @param id 主键id
     * @return 前台用户公开信息
     */
    @Operation(summary = "查询用户公开信息", description = "按ID查询账号与头像，供评论区展示他人身份；手机号、邮箱等私密资料不在此返回")
    @GetMapping(value = "/{id}")
    public R<ConsumerPublicVo> selectByPrimaryKey(@Parameter(description = "用户ID", required = true, example = "1") @PathVariable("id") Integer id) {
        Consumer consumer = consumerService.selectByPrimaryKey(id);
        if (consumer == null) {
            // 保持"查不到即空载荷"的原语义，不改成 error：评论区展示他人身份时
            // 账号可能已注销，这不是调用方的错误
            return R.ok(null);
        }
        ConsumerPublicVo publicVo = new ConsumerPublicVo();
        publicVo.setId(consumer.getId());
        publicVo.setUsername(consumer.getUsername());
        publicVo.setAvator(consumer.getAvator());
        return R.ok(publicVo);
    }

    /**
     * 查询本人完整资料
     * 不接受 id 参数：身份只取自令牌，否则可通过遍历 id 读到他人手机号与邮箱
     *
     * @param currentUserId 令牌中的用户id
     * @return 当前用户的完整资料
     */
    @Operation(summary = "查询本人资料", description = "返回当前令牌用户的完整资料，含私密字段")
    @GetMapping(value = "/profile")
    public R<Consumer> profile(@RequestAttribute("userId") Integer currentUserId) {
        return R.ok(consumerService.selectByPrimaryKey(currentUserId));
    }

    /**
     * 查询所有前台用户
     *
     * @return 前台用户
     */
    @Operation(summary = "查询所有用户", description = "查询所有前台用户列表")
    @GetMapping
    @RequireUserType("admin")
    public R<List<Consumer>> allConsumer() {
        return R.ok(consumerService.allConsumer());
    }
    
    /**
     * 分页查询所有前台用户
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @Operation(summary = "分页查询用户", description = "分页查询所有前台用户")
    @GetMapping(value = "/page")
    @RequireUserType("admin")
    public R<PageInfo<Consumer>> allConsumerByPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int pageSize
    ) {
        PageInfo<Consumer> pageInfo = consumerService.allConsumerByPage(pageNum, pageSize);
        return R.ok(pageInfo);
    }

    /**
     * 更新前台用户头像
     *
     * @param avatorFile    头像文件
     * @param id            用户id
     * @param currentUserId 令牌中的用户id
     * @param userType      令牌中的用户类型
     * @return 上传结果
     */
    @RateLimit(key = "upload:avatar:", timeWindow = 60, maxRequests = 5, message = "头像上传过于频繁，请稍后再试")
    @PostMapping(value = "/{id}/avatar")
    @RequireUserType("consumer")
    public R<UploadConsumerPicVo> updateConsumerPic(
            @RequestParam("file") MultipartFile avatorFile,
            @PathVariable("id") Integer id,
            @RequestAttribute("userId") Integer currentUserId,
            @RequestAttribute("userType") String userType,
            @RequestAttribute("username") String username,
            HttpServletRequest request
    ) throws IOException {
        // 管理端可代改他人头像，普通用户只能改自己的。
        // 校验放在落盘之前，否则越权请求也会先把文件写进磁盘
        if (!currentUserId.equals(id) && !"admin".equals(userType)) {
            log.warn("头像更新被拒 - 令牌用户={}, 目标用户={}", currentUserId, id);
            return R.error("无权修改他人头像");
        }
        String storeAvatorPath;
        try {
            storeAvatorPath = fileStorageService.saveImage(avatorFile, "userImages");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return R.error(e.getMessage());
        }
        Consumer consumer = new Consumer();
        consumer.setId(id);
        consumer.setAvator(storeAvatorPath);

        UploadConsumerPicVo uploadConsumerPicVo = new UploadConsumerPicVo();
        uploadConsumerPicVo.setAvator(storeAvatorPath);
        //更新前端用户
        if (consumerService.update(consumer)) {
            operationLogService.logUpdate(currentUserId, username, userType,
                    "用户模块", "更新头像 targetUserId=" + id, ipUtils.getIpAddress(request));
            return R.ok("上传成功", uploadConsumerPicVo);
        }

        return R.error("上传失败");
    }

    /**
     * 前台用户登录(JWT + IP绑定)
     *
     * @param username 用户名
     * @param password 用户密码
     * @param request  HttpServletRequest
     * @return 登录用户信息和Token
     */
    @PostMapping(value = "/sessions")
    @RateLimit(key = "login:consumer:", timeWindow = 60, maxRequests = 5, message = "登录尝试次数过多，请60秒后再试")
    public R<ConsumerLoginVo> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return R.error("用户名或者密码不能为空");
        }

        //获取客户端IP地址
        String ip = ipUtils.getIpAddress(request);

        //验证用户名和密码
        if (!consumerService.verifyPassword(username, password)) {
            log.warn("用户登录失败 - username={}, ip={}", username, ip);
            operationLogService.logLogin(null, username, "consumer", ip, false);
            return R.error("用户名或密码错误");
        }

        //获取用户信息
        Consumer consumer = consumerService.getByUsername(username);
        if (consumer == null) {
            return R.error("用户不存在");
        }

        //生成JWT Token(包含用户ID、用户名、用户类型、IP地址)
        String token = jwtUtils.generateToken(consumer.getId(), username, "consumer", ip);

        //构建返回结果
        ConsumerLoginVo result = new ConsumerLoginVo();
        result.setUserMsg(consumer);
        result.setToken(token);
        // 与 jwt.expiration 同源，不再各写一份
        result.setExpiresIn(jwtUtils.getExpirationSeconds());

        operationLogService.logLogin(consumer.getId(), username, "consumer", ip, true);
        log.info("用户登录成功 - userId={}, username={}, ip={}", consumer.getId(), username, ip);
        return R.ok("登录成功", result);
    }

}
