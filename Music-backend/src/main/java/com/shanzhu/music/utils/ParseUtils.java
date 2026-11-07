package com.shanzhu.music.utils;

/**
 * 请求参数解析工具。
 *
 * 项目里多处把前端传来的字符串直接 Integer.parseInt，一旦为空或非数字就抛
 * NumberFormatException，冒到接口层变成 500 且对用户没有任何有效提示。
 */
public final class ParseUtils {

    private ParseUtils() {
    }

    /**
     * 解析为 Integer，为空或非法时返回 null，由调用方转成可读的错误提示
     */
    public static Integer toIntegerOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
