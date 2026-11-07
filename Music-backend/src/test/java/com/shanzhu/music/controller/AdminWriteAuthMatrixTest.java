package com.shanzhu.music.controller;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

/**
 * 全部标注 @RequireUserType("admin") 接口的鉴权矩阵。
 *
 * 补这个类的原因很直接：22 处标注里此前只有 /song/manage/add 一处被用例验证过，
 * 其余 21 处一旦漏写注解、拼错角色值或改错路径，146 项测试不会有任何一项变红。
 * 基类预置的是管理员令牌，"权限没生效"在这种测试结构下天然不可见。
 *
 * 路径与注解的对应关系由端点清单显式列出。新增管理接口时必须往这里加一行，
 * 没加就是显式缺口，好过悄悄漏测。
 */
class AdminWriteAuthMatrixTest extends BaseTest {

    /**
     * 基类的 mockMvc 带管理员令牌，用它测不出鉴权；这里另建一个无默认请求的实例。
     */
    private MockMvc anonymous;

    @BeforeEach
    void setUpAnonymousMockMvc() {
        this.anonymous = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    /**
     * 端点清单：{HTTP 方法, 完整路径, 是否 multipart}。
     * multipart 端点不带文件发起请求即可——鉴权发生在参数解析之前，
     * 构造文件只会让这 44 个用例变慢，不改变被验证的东西。
     */
    static Stream<Arguments> adminEndpoints() {
        return Stream.of(
                // 曲库：歌曲
                Arguments.of("POST", "/song/manage/add", true),
                Arguments.of("POST", "/song/manage/update", false),
                Arguments.of("DELETE", "/song/manage/delete", false),
                Arguments.of("POST", "/song/manage/updateSongPic", true),
                Arguments.of("POST", "/song/manage/updateSongUrl", true),
                // 曲库：歌手
                Arguments.of("POST", "/singer/add", false),
                Arguments.of("POST", "/singer/update", false),
                Arguments.of("DELETE", "/singer/delete", false),
                Arguments.of("POST", "/singer/updateSingerPic", true),
                // 曲库：歌单
                Arguments.of("POST", "/songList/add", false),
                Arguments.of("POST", "/songList/update", false),
                Arguments.of("DELETE", "/songList/delete", false),
                Arguments.of("POST", "/songList/updateSongListPic", true),
                // 歌单与歌曲的关联
                Arguments.of("POST", "/listSong/add", false),
                Arguments.of("DELETE", "/listSong/delete", false),
                // 标签
                Arguments.of("POST", "/tag/song/update", false),
                Arguments.of("POST", "/tag/singer/update", false),
                Arguments.of("POST", "/tag/batchAdd", false),
                // 用户治理：删除账号与两份全量名单
                Arguments.of("DELETE", "/consumer/delete", false),
                Arguments.of("GET", "/consumer/allConsumer", false),
                Arguments.of("GET", "/consumer/allConsumerByPage", false),
                Arguments.of("GET", "/collect/allCollect", false));
    }

    @ParameterizedTest(name = "匿名调用 {0} {1} 应被拒 (第 {index} 项)")
    @MethodSource("adminEndpoints")
    void anonymousCannotReachAdminEndpoints(String method, String path, boolean multipart) throws Exception {
        anonymous.perform(requestFor(method, path, multipart))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    /**
     * 消费者令牌必须拿到 403 而不是 401 或 404。
     *
     * 断 403 顺带校验了路径本身正确：若这里拼错路径，请求命中的是静态资源兜底而非
     * 目标方法，角色校验根本不会触发，返回会是 404——用例当场失败。
     * 而匿名那一半只断 401，未匹配到 handler 时同样返回 401，暴露不了拼写错误。
     * 两半合起来才是完整覆盖，所以缺一不可。
     */
    @ParameterizedTest(name = "消费者令牌调用 {0} {1} 应被拒 (第 {index} 项)")
    @MethodSource("adminEndpoints")
    void consumerTokenCannotReachAdminEndpoints(String method, String path, boolean multipart) throws Exception {
        mockMvc.perform(requestFor(method, path, multipart)
                        .header("Authorization", consumerToken()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    private MockHttpServletRequestBuilder requestFor(String method, String path, boolean multipart) {
        return switch (method) {
            case "GET" -> MockMvcRequestBuilders.get(path);
            case "DELETE" -> MockMvcRequestBuilders.delete(path);
            case "POST" -> multipart
                    ? MockMvcRequestBuilders.multipart(path)
                    : MockMvcRequestBuilders.post(path);
            default -> throw new IllegalArgumentException("矩阵未覆盖的 HTTP 方法: " + method);
        };
    }

    /**
     * 只允许 consumer 的自助写接口。
     *
     * 评论 delete 允许 {consumer, admin}（管理员可清理违规评论，归属校验在 service 里），
     * 因此不列在这里——用 admin 令牌打它应当是 200 而不是 403。
     * 评论正文没有 update 接口：发布后连作者也不能修改。
     */
    static Stream<Arguments> consumerOnlyEndpoints() {
        return Stream.of(
                Arguments.of("POST", "/collect/add", false),
                Arguments.of("DELETE", "/collect/delete", false),
                Arguments.of("POST", "/comment/add", false),
                Arguments.of("POST", "/comment/like", false),
                Arguments.of("POST", "/consumer/update", false),
                Arguments.of("POST", "/consumer/updateConsumerPic", true),
                Arguments.of("POST", "/rank/add", false),
                Arguments.of("POST", "/search-history/record", false),
                Arguments.of("DELETE", "/search-history/clear", false),
                Arguments.of("DELETE", "/search-history/clean-old", false));
    }

    @ParameterizedTest(name = "匿名调用消费者接口 {0} {1} 应被拒 (第 {index} 项)")
    @MethodSource("consumerOnlyEndpoints")
    void anonymousCannotReachConsumerEndpoints(String method, String path, boolean multipart) throws Exception {
        anonymous.perform(requestFor(method, path, multipart))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    /**
     * 管理员令牌也不能替消费者写自助数据。
     *
     * 这条看着像多余，实际不是：评论、收藏、评分都以 consumer 表的 id 为归属，
     * admin 的 id 在 consumer 表里不存在，让它写这类数据只会造成孤儿行或外键失败。
     * fail-closed 之后角色域是显式声明的，这里验证声明确实生效。
     */
    @ParameterizedTest(name = "管理员令牌调用消费者接口 {0} {1} 应被拒 (第 {index} 项)")
    @MethodSource("consumerOnlyEndpoints")
    void adminTokenCannotReachConsumerEndpoints(String method, String path, boolean multipart) throws Exception {
        mockMvc.perform(requestFor(method, path, multipart))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}
