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
        assertEquals(testData, result.getData());
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
     * 测试链式调用
     */
    @Test
    @Order(10)
    @DisplayName("测试链式调用")
    void testChaining() {
        R result = R.ok()
                .code(20000)
                .message("自定义消息")
                .success(true);
        
        assertNotNull(result);
        assertEquals(20000, result.getCode());
        assertEquals("自定义消息", result.getMessage());
        assertTrue(result.getSuccess());
        System.out.println("链式调用: " + result);
    }

    /**
     * 测试 data 方法(Map)
     */
    @Test
    @Order(11)
    @DisplayName("测试data方法-Map")
    void testDataWithMap() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("total", 100);
        dataMap.put("list", new String[]{"item1", "item2"});
        
        R result = R.ok().data(dataMap);
        
        assertNotNull(result);
        assertEquals(dataMap, result.getDataMap());
        System.out.println("R.data(map): " + result.getDataMap());
    }

    /**
     * 测试 data 方法(key-value)
     */
    @Test
    @Order(12)
    @DisplayName("测试data方法-key-value")
    void testDataWithKeyValue() {
        R result = R.ok()
                .data("key1", "value1")
                .data("key2", 123);
        
        assertNotNull(result);
        assertEquals("value1", result.getDataMap().get("key1"));
        assertEquals(123, result.getDataMap().get("key2"));
        System.out.println("R.data(key, value): " + result.getDataMap());
    }

    /**
     * 测试泛型类型安全
     */
    @Test
    @Order(13)
    @DisplayName("测试泛型类型安全")
    void testGenericTypeSafety() {
        // 字符串类型
        R<String> stringResult = R.ok("test");
        String data = stringResult.getData();
        assertEquals("test", data);
        
        // 整数类型
        R<Integer> intResult = R.ok(123);
        Integer intData = intResult.getData();
        assertEquals(123, intData);
        
        System.out.println("泛型测试通过");
    }
}
