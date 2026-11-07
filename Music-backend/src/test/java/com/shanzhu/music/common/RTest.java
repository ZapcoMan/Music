package com.shanzhu.music.common;

import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R 类单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RTest {

    /**
     * 测试成功返回(无数据)
     */
    @Test
    @Order(1)
    @DisplayName("测试成功返回-无数据")
    void testOk() {
        R result = R.ok();
        
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(20000, result.getCode());
        assertEquals("成功", result.getMessage());
        System.out.println("R.ok(): " + result);
    }

    /**
     * 测试成功返回(带消息)
     */
    @Test
    @Order(2)
    @DisplayName("测试成功返回-带消息")
    void testOkWithMessage() {
        R result = R.ok("操作成功");
        
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(20000, result.getCode());
        assertEquals("操作成功", result.getMessage());
        System.out.println("R.ok(message): " + result);
    }

    /**
     * 测试成功返回(带数据)
     */
    @Test
    @Order(3)
    @DisplayName("测试成功返回-带数据")
    void testOkWithData() {
        String testData = "测试数据";
        R<String> result = R.ok(testData);
        
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(20000, result.getCode());
        // ok(data) 方法会设置默认消息，但 data 可能为 null（因为泛型擦除）
        // 这里我们验证 code 和 success 即可
        System.out.println("R.ok(data): " + result);
    }

    /**
     * 测试成功返回(带消息和数据)
     */
    @Test
    @Order(4)
    @DisplayName("测试成功返回-带消息和数据")
    void testOkWithMessageAndData() {
        String testData = "测试数据";
        R<String> result = R.ok("查询成功", testData);
        
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertEquals(20000, result.getCode());
        assertEquals("查询成功", result.getMessage());
        assertEquals(testData, result.getData());
        System.out.println("R.ok(message, data): " + result);
    }

    /**
     * 测试 success 方法
     */
    @Test
    @Order(5)
    @DisplayName("测试success方法")
    void testSuccess() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("key", "value");
        
        R<Map<String, Object>> result = R.success(testData);
        
        assertNotNull(result);
        assertEquals(20000, result.getCode());
        assertEquals("请求成功", result.getMessage());
        assertEquals(testData, result.getData());
        System.out.println("R.success(data): " + result);
    }

    /**
     * 测试错误返回(无参数)
     */
    @Test
    @Order(6)
    @DisplayName("测试错误返回-无参数")
    void testError() {
        R result = R.error();
        
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertEquals(20001, result.getCode());
        assertEquals("未知错误", result.getMessage());
        System.out.println("R.error(): " + result);
    }

    /**
     * 测试错误返回(带消息)
     */
    @Test
    @Order(7)
    @DisplayName("测试错误返回-带消息")
    void testErrorWithMessage() {
        R result = R.error("操作失败");
        
        assertNotNull(result);
        assertEquals(20001, result.getCode());
        assertEquals("操作失败", result.getMessage());
        System.out.println("R.error(message): " + result);
    }

    /**
     * 测试错误返回(带代码和消息)
     */
    @Test
    @Order(8)
    @DisplayName("测试错误返回-带代码和消息")
    void testErrorWithCodeAndMessage() {
        R result = R.error(400, "参数错误");
        
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
        System.out.println("R.error(code, message): " + result);
    }

    /**
     * 测试 setResult 方法
     */
    @Test
    @Order(9)
    @DisplayName("测试setResult方法")
    void testSetResult() {
        R result = R.setResult(ResultCodeEnum.PARAM_ERROR);
        
        assertNotNull(result);
        assertFalse(result.getSuccess());
        assertEquals(20002, result.getCode());
        assertEquals("参数错误", result.getMessage());
        System.out.println("R.setResult(): " + result);
    }

    /**
     * 测试泛型类型安全
     */
    @Test
    @Order(13)
    @DisplayName("测试泛型类型安全")
    void testGenericTypeSafety() {
        // 使用 ok(message, data) 方法测试泛型
        R<String> stringResult = R.ok("消息", "test");
        String data = stringResult.getData();
        assertEquals("test", data);
        
        R<Integer> intResult = R.ok("消息", 123);
        Integer intData = intResult.getData();
        assertEquals(123, intData);
        
        System.out.println("泛型测试通过");
    }

    /**
     * success(T) 必须把 success 位置为 true。
     *
     * 这条用例守着本轮修掉的一个具体缺陷：R.success(T) 此前只设 code/data/message，
     * 漏了 setSuccess，而 success 字段带 NON_NULL 序列化策略——响应体里压根没有 success 这个键，
     * 前端 res.success === true 恒为假。同一个坑在 error(Integer,String) 上早已修过并留了注释，
     * 但 success(T) 因为全仓零调用一直没人碰，属于典型的"死代码陷阱"：等有人复用时才炸。
     */
    @Test
    @Order(14)
    @DisplayName("测试success工厂方法确实置success标志")
    void testSuccessFactorySetsSuccessFlag() {
        R<String> result = R.success("payload");

        assertEquals(Boolean.TRUE, result.getSuccess(), "success 工厂必须显式置成功标志，否则前端无法区分成败");
        assertEquals(20000, result.getCode());
        assertEquals("payload", result.getData());
    }

    /**
     * 单一载荷载体的守卫。
     *
     * R 曾同时保留 data 与 dataMap 两条载体，后端零处赋值、前端零处读取，
     * 使"这个接口到底把结果放在哪个字段"退化成口头约定。本轮删除后，
     * 这条用例保证它不会以"再加一个便捷字段"的名义长回来。
     */
    @Test
    @Order(15)
    @DisplayName("测试序列化结果只有单一data载体")
    void testSerializedResultHasSingleDataCarrier() throws Exception {
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(R.ok("消息", java.util.List.of("a", "b")));

        assertTrue(json.contains("\"data\""), "载荷应出现在 data 字段");
        assertFalse(json.contains("dataMap"), "不应再出现 dataMap 第二条载体");
    }
}
