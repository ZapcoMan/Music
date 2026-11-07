package com.shanzhu.music;

import com.shanzhu.music.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 基础测试类
 * 所有测试类继承此类,自动加载 Spring 上下文。
 *
 * classes 必须显式写在这里：@SpringBootTest 未指定 classes 时，
 * 上下文缓存键取"被测类自身"，于是 18 个测试类各自新建一个上下文，
 * 每个约 41 秒 —— 这正是全量 mvn test 跑不完的直接原因。
 * 指定同一个配置类后，所有子类共用一个上下文，只启动一次。
 * @AutoConfigureMockMvc 同样上移，避免子类各写一份造成键不一致。
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class BaseTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected JwtUtils jwtUtils;

    /**
     * 统一在基类构造，并给所有请求预置一个合法的管理员令牌。
     *
     * 两个理由：
     * 1) 本项目自强制鉴权后，不带 Token 的业务接口一律 401 ——
     *    各测试类此前 @Autowired 的 MockMvc 实际上只测到了拦截器，
     *    断言的 200 全部来自 401 之外的路径，根本没进过业务层；
     * 2) defaultRequest 让令牌只在这一处出现，48 个用例无需各自重复带头。
     *    需要验证"未登录被拒"的用请另外显式构造不带令牌的请求，
     *    不该被这里的默认值掩盖。
     */
    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", adminToken()))
                .build();
    }

    /**
     * 管理员令牌：多数接口需要 admin 角色；userType 与真实登录签发的一致
     */
    protected String adminToken() {
        return jwtUtils.generateToken(1, "admin", "admin", "127.0.0.1");
    }

    /**
     * 前台用户令牌，用于校验角色隔离的用例
     */
    protected String consumerToken() {
        return jwtUtils.generateToken(35, "user", "consumer", "127.0.0.1");
    }
}
