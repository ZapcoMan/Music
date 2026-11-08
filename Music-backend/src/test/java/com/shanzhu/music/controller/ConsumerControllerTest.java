package com.shanzhu.music.controller;

import cn.hutool.core.date.DateUtil;
import com.shanzhu.music.BaseTest;
import com.shanzhu.music.entity.po.Consumer;
import com.shanzhu.music.service.ConsumerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConsumerController 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConsumerControllerTest extends BaseTest {

    /**
     * 注册与资料修改的逻辑已下沉到 service，这里要用它读回真实落库的结果，
     * 而不是只看响应体里那个布尔值。
     */
    @Autowired
    private ConsumerService consumerService;

    /**
     * 测试用户登录 - 成功
     */
    @Test
    @Order(1)
    @DisplayName("测试用户登录-成功")
    void testLoginSuccess() throws Exception {
        // ConsumerController 中的登录接口是 /consumer/login
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/sessions")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "123456"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
        
        System.out.println("用户登录成功接口测试通过");
    }

    /**
     * 测试用户登录 - 失败
     */
    @Test
    @Order(2)
    @DisplayName("测试用户登录-失败")
    void testLoginFailed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/sessions")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "wrongpassword"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
        
        System.out.println("用户登录失败接口测试通过");
    }

    /**
     * 测试用户注册
     *
     * 查重、生日解析、DTO→PO 组装都在 service 里完成，所以断的是落库结果：
     * 只断 $.success 存在的话，"注册失败"与"注册成功但生日没存进去"都能通过。
     */
    @Test
    @Order(3)
    @DisplayName("测试用户注册")
    void testSignUp() throws Exception {
        // 用户名带时间戳：Redis 是本机共享的，写死用户名的话上一轮跑完留下的
        // music:consumer:username:* 缓存会让这一轮的注册直接被"已存在"拦下
        String username = "newuser" + System.currentTimeMillis();
        String jsonRequest = "{" +
                "\"username\":\"" + username + "\"," +
                "\"password\":\"123456\"," +
                "\"sex\":1," +
                "\"phoneNum\":\"13800138002\"," +
                "\"email\":\"newuser@example.com\"," +
                "\"birth\":\"1995-06-15\"," +
                "\"introduction\":\"新用户\"," +
                "\"location\":\"深圳\"," +
                "\"avator\":\"/userImages/test.jpg\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Consumer registered = consumerService.getByUsername(username);
        assertNotNull(registered, "注册后应能按账号查回用户");
        // 请求里是 "1995-06-15" 字符串，库里是 date 列，转换在 service 的 toConsumer 里
        assertEquals("1995-06-15", DateUtil.formatDate(registered.getBirth()),
                "生日应解析成 date 落库，而不是丢掉或按当前时间填充");
        assertTrue(registered.getPassword().startsWith("$2"),
                "密码必须加密存储，注册接口不能落明文");
        assertEquals("", registered.getAvator(),
                "头像由上传接口维护，注册时只占空串（请求里的 avator 字段不采纳）");

        // 同名再注册应被拒，且第一次注册的数据不被二次写入覆盖
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("用户名已存在"));

        assertEquals("新用户", consumerService.getByUsername(username).getIntroduction(),
                "重复注册不应改动已存在的账号");
    }

    /**
     * 测试查询用户详情
     */
    @Test
    @Order(4)
    @DisplayName("测试查询用户详情")
    void testSelectByPrimaryKey() throws Exception {
        // ConsumerController 中的接口是 /consumer/selectByPrimaryKey
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/consumers/1")
                .param("id", "1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
        
        System.out.println("查询用户详情接口测试通过");
    }

    /**
     * 测试修改用户信息
     *
     * 上一版用 consumerMvc 的默认令牌（写死 userId=35）直接打这个接口，而测试库的 consumer
     * 表是空的——服务端返回的一直是"用户不存在"，但用例只断言 $.success 存在，
     * 于是它从没测过一次真正的修改。这里先注册出一个真实账号，再用它自己的令牌改。
     */
    @Test
    @Order(5)
    @DisplayName("测试修改用户信息")
    void testUpdateConsumer() throws Exception {
        String username = "profileuser" + System.currentTimeMillis();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Consumer created = consumerService.getByUsername(username);
        assertNotNull(created, "前置账号应已注册");

        String jsonRequest = "{" +
                "\"username\":\"hijacked\"," +
                "\"password\":\"123456\"," +
                "\"sex\":1," +
                "\"phoneNum\":\"13800138000\"," +
                "\"email\":\"test@example.com\"," +
                "\"birth\":\"1990-01-01\"," +
                "\"introduction\":\"修改后的简介\"," +
                "\"location\":\"广州\"," +
                "\"avator\":\"/userImages/test.jpg\"" +
                "}";

        consumerMvc.perform(MockMvcRequestBuilders.patch("/api/v1/consumers/profile")
                .header("Authorization",
                        jwtUtils.generateToken(created.getId(), created.getUsername(), "consumer", "127.0.0.1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Consumer updated = consumerService.selectByPrimaryKey(created.getId());
        assertEquals("修改后的简介", updated.getIntroduction(), "资料修改应落库");
        assertEquals("广州", updated.getLocation());
        assertEquals("1990-01-01", DateUtil.formatDate(updated.getBirth()),
                "生日字符串应在 service 里解析成 date，改资料时不能丢");
        // 请求里谎称 username=hijacked，但用户名是登录凭据，不随资料接口改动
        assertEquals(username, updated.getUsername());
        assertNotEquals("hijacked", updated.getUsername(), "用户名不可通过资料接口改名");
    }

    /**
     * 改资料时不带 password，必须改得动、且原口令不受影响。
     *
     * 上面那条用例的 JSON 里一直带着 password，恰好绕开了本接口最常见的用法：
     * 用户只想改签名或地区。此前 profile 复用注册用的 ConsumerRequest，
     * 它的 @NotBlank(password) 会在请求进入 service 之前就把这种提交判成 400，
     * 前端"留空即不发送该字段、后端保持原密码"的约定根本没有机会生效。
     *
     * 断言走 verifyPassword 而不是比对散列原值：selectByPrimaryKey 会命中
     * 用户缓存，而 Consumer 的 password 带 @JsonIgnore，缓存里反序列化出来
     * 根本没有这个字段（见 ConsumerServiceImpl#verifyPassword 的说明）。
     */
    @Test
    @Order(91)
    @DisplayName("测试修改资料时不提交密码")
    void testUpdateProfileWithoutPassword() throws Exception {
        String username = "nopassuser" + System.currentTimeMillis();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Consumer created = consumerService.getByUsername(username);
        Assertions.assertNotNull(created, "前置账号应已注册");

        consumerMvc.perform(MockMvcRequestBuilders.patch("/api/v1/consumers/profile")
                .header("Authorization",
                        jwtUtils.generateToken(created.getId(), created.getUsername(), "consumer", "127.0.0.1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"introduction\":\"只改签名，不动密码\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Consumer updated = consumerService.selectByPrimaryKey(created.getId());
        Assertions.assertEquals("只改签名，不动密码", updated.getIntroduction(),
                "只提交 introduction 时该字段应落库");
        Assertions.assertTrue(consumerService.verifyPassword(username, "123456"),
                "未提交 password 时原口令必须仍然可用，不能被清空或覆盖");
        Assertions.assertFalse(consumerService.verifyPassword(username, ""),
                "password 缺省不应被当作\"把口令改成空串\"");
    }

    /**
     * 测试分页查询用户
     */
    @Test
    @Order(6)
    @DisplayName("测试分页查询用户")
    void testAllConsumerByPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/consumers/page")
                .param("pageNum", "1")
                .param("pageSize", "5"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray());
        
        System.out.println("分页查询用户接口测试通过");
    }

    /**
     * 测试删除用户
     */
    @Test
    @Order(7)
    @DisplayName("测试删除用户")
    void testDeleteConsumer() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/consumers/1")
                .param("id", "99999"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").exists());
        
        System.out.println("删除用户接口测试通过");
    }

    @Test
    @Order(9)
    @DisplayName("测试参数校验失败的 code 取自枚举而非 400")
    void testParamValidationErrorUsesEnumCode() throws Exception {
        // 同一个"参数错误"语义此前有两个 code 来源：ResultCodeEnum.PARAM_ERROR 是 20002，
        // 而 GlobalExceptionHandler 直接写了 R.error(400, ...)
        String jsonRequest = "{\"username\":\"\",\"password\":\"123456\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/consumers/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(20002))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名不能为空")));
    }
}
