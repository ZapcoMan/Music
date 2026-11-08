package com.shanzhu.music;

import com.shanzhu.music.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

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

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

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

    /**
     * 预置消费者令牌的实例。
     *
     * 评论、收藏、评分、搜索历史这些接口按角色只允许 consumer，写接口又改为 fail-closed：
     * 用基类默认的 admin 实例打它们，拿到的是 403，测到的是权限模型而不是业务逻辑本身。
     * 单列一个实例，免得每个用例都手写一遍 header。
     */
    protected MockMvc consumerMvc;

    @BeforeEach
    void setUpMockMvc() {
        evictAppCache();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", adminToken()))
                .build();
        this.consumerMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", consumerToken()))
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

    /**
     * 每个用例前清空本应用的缓存与限流计数器，让本地跑法与 CI 的冷缓存跑法对齐。
     *
     * 不清缓存的后果已经出现：测试库的自增 id 每轮都从 1 重新开始，而本机 Redis 是跨轮存活的，
     * 上一轮写进 music:consumer:3 的用户会被这一轮的 selectByPrimaryKey(3) 原样读出，
     * 用例拿到的是"别人家的行"——表现就是本地偶发红、CI 全绿（或反过来）。
     *
     * 限流计数器（rate:*）同样要清，理由不同但更容易出事：它按"注解前缀 + 身份"累计，
     * 同一身份在整轮测试里共用一个 60 秒窗口。评论接口 10 次/60 秒，
     * 一个类里多几条用例就先撞上 429，之后断言测的是限流而不是被测逻辑。
     * 清理发生在每个用例之前，因此专门验证限流的用例依然成立——
     * 那类用例是在一次调用里连续打满窗口，不依赖跨用例残留。
     *
     * 键模式只有 music:* 与 rate:*：分别是业务缓存与限流的命名空间，不会误伤同机其他应用。
     * 已吊销令牌黑名单在 jwt:blacklist: 下，它不是缓存，清掉等于把登出过的令牌放回来，绝不动。
     *
     * KEYS 在大集合上会阻塞 Redis，所以这条路只写在测试里，不进生产代码。
     */
    private void evictAppCache() {
        for (String pattern : new String[]{"music:*", "rate:*"}) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }
}
