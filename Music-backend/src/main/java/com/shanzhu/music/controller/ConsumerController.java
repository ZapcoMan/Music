package com.shanzhu.music.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.annotation.RateLimit;
import com.shanzhu.music.annotation.RequireUserType;
import com.shanzhu.music.common.R;
import com.shanzhu.music.entity.dto.ConsumerRequest;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.entity.vo.ConsumerPublicVo;
import com.shanzhu.music.entity.vo.UploadConsumerPicVo;
import com.shanzhu.music.service.ConsumerService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台用户相关 控制层
 *
 * @author: ShanZhu
 * @date: 2023-11-25
 */
@Slf4j
@RestController
@RequestMapping("/consumer")
@RequiredArgsConstructor
@Tag(name = "前台用户管理", description = "前台用户注册、登录、信息查询等接口")
public class ConsumerController {

    private final ConsumerService consumerService;
    private final JwtUtils jwtUtils;
    private final IpUtils ipUtils;
    private final TokenBlacklistService tokenBlacklistService;
    private final FileStorageService fileStorageService;

    /**
     * 退出登录：吊销当前令牌，使其在有效期内立即不可用。
     * 此前只由前端删除本地 token，泄漏出去的令牌在服务端仍然有效。
     *
     * @param request HttpServletRequest
     * @return 结果
     */
    @Operation(summary = "用户登出", description = "使当前令牌立即失效")
    @PostMapping(value = "/logout")
    public R logout(HttpServletRequest request) {
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
     * 添加前台用户
     *
     * @param request 用户信息
     * @return 添加结果
     */
    @Operation(summary = "注册用户", description = "注册新的前台用户账号")
    @PostMapping(value = "/add")
    public R addConsumer(@Valid @RequestBody ConsumerRequest request) {
        //已经存在用户名
        Consumer existConsumer = consumerService.getByUsername(request.getUsername());
        if (existConsumer != null) {
            return R.error("用户名已存在");
        }

        //把生日转换成Date格式
        DateTime birthDate = null;
        if (request.getBirth() != null && !request.getBirth().isEmpty()) {
            birthDate = DateUtil.parse(request.getBirth(), DatePattern.NORM_DATE_PATTERN);
        }

        //保存到前端用户的对象中
        Consumer consumer = new Consumer();
        consumer.setUsername(request.getUsername());
        consumer.setPassword(request.getPassword());
        consumer.setSex(request.getSex());
        consumer.setPhoneNum(request.getPhoneNum());
        consumer.setEmail(request.getEmail());
        consumer.setBirth(birthDate);
        consumer.setIntroduction(request.getIntroduction());
        consumer.setLocation(request.getLocation());
        consumer.setAvator("");

        //保存成功
        if (consumerService.insert(consumer)) {
            return R.ok("添加成功");
        }
        return R.error("添加失败");
    }

    /**
     * 修改前台用户
     *
     * @param request 用户信息
     * @return 修改结果
     */
    @PostMapping(value = "/update")
    public R updateConsumer(@RequestAttribute("userId") Integer currentUserId,
                            @Valid @RequestBody ConsumerRequest request) {
        //把生日转换成Date格式
        DateTime birthDate = null;
        if (request.getBirth() != null && !request.getBirth().isEmpty()) {
            birthDate = DateUtil.parse(request.getBirth(), DatePattern.NORM_DATE_PATTERN);
        }

        // 以令牌身份为准：原实现按 username 反查用户，任何人提交他人用户名即可改其资料
        Consumer existConsumer = consumerService.selectByPrimaryKey(currentUserId);
        if (existConsumer == null) {
            return R.error("用户不存在");
        }

        //保存到前端用户的对象中
        Consumer consumer = new Consumer();
        consumer.setId(currentUserId);
        // 用户名是登录凭据，不允许通过本接口改名
        consumer.setUsername(existConsumer.getUsername());
        consumer.setPassword(request.getPassword());
        consumer.setSex(request.getSex());
        consumer.setPhoneNum(request.getPhoneNum());
        consumer.setEmail(request.getEmail());
        consumer.setBirth(birthDate);
        consumer.setIntroduction(request.getIntroduction());
        consumer.setLocation(request.getLocation());

        //保存成功
        if (consumerService.update(consumer)) {
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
    @DeleteMapping(value = "/delete")
    @RequireUserType("admin")
    public Boolean deleteConsumer(Integer id) {
        return consumerService.delete(id);
    }

    /**
     * 查询用户公开信息
     *
     * @param id 主键id
     * @return 前台用户公开信息
     */
    @Operation(summary = "查询用户公开信息", description = "按ID查询账号与头像，供评论区展示他人身份；手机号、邮箱等私密资料不在此返回")
    @GetMapping(value = "/selectByPrimaryKey")
    public ConsumerPublicVo selectByPrimaryKey(@Parameter(description = "用户ID", required = true, example = "1") Integer id) {
        Consumer consumer = consumerService.selectByPrimaryKey(id);
        if (consumer == null) {
            return null;
        }
        ConsumerPublicVo publicVo = new ConsumerPublicVo();
        publicVo.setId(consumer.getId());
        publicVo.setUsername(consumer.getUsername());
        publicVo.setAvator(consumer.getAvator());
        return publicVo;
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
    public Consumer profile(@RequestAttribute("userId") Integer currentUserId) {
        return consumerService.selectByPrimaryKey(currentUserId);
    }

    /**
     * 查询所有前台用户
     *
     * @return 前台用户
     */
    @Operation(summary = "查询所有用户", description = "查询所有前台用户列表")
    @GetMapping(value = "/allConsumer")
    @RequireUserType("admin")
    public List<Consumer> allConsumer() {
        return consumerService.allConsumer();
    }
    
    /**
     * 分页查询所有前台用户
     *
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 分页结果
     */
    @Operation(summary = "分页查询用户", description = "分页查询所有前台用户")
    @GetMapping(value = "/allConsumerByPage")
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
    @PostMapping(value = "/updateConsumerPic")
    public R<UploadConsumerPicVo> updateConsumerPic(
            @RequestParam("file") MultipartFile avatorFile,
            @RequestParam("id") Integer id,
            @RequestAttribute("userId") Integer currentUserId,
            @RequestAttribute("userType") String userType
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
    @PostMapping(value = "/login")
    @RateLimit(key = "login:consumer:", timeWindow = 60, maxRequests = 5, message = "登录尝试次数过多，请60秒后再试")
    public R<Map<String, Object>> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return R.error("用户名或者密码不能为空");
        }

        //获取客户端IP地址
        String ip = ipUtils.getIpAddress(request);

        //验证用户名和密码
        if (!consumerService.verifyPassword(username, password)) {
            log.warn("用户登录失败 - username={}, ip={}", username, ip);
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
        Map<String, Object> result = new HashMap<>();
        result.put("userMsg", consumer);
        result.put("token", token);
        result.put("expiresIn", 86400); // Token过期时间(秒)

        log.info("用户登录成功 - userId={}, username={}, ip={}", consumer.getId(), username, ip);
        return R.ok("登录成功", result);
    }

}
