package com.shanzhu.music.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类 - 使用BCrypt算法
 */
@Component
public class PasswordUtils {

    /**
     * cost 取 12。默认构造是 10，按现行 OWASP 建议在离线爆破面前抵抗窗口偏小。
     *
     * 不需要迁移既有散列：BCrypt 把 cost 编在盐里（形如 $2a$10$...），
     * matches 会按散列自身记录的 cost 校验，所以老账号继续能登录，
     * 只有新建与改密的账号才落到 12。强行全员改密反而会制造"必须重置"的运维事故。
     *
     * 代价是单次哈希耗时约 4 倍，登录与注册路径上都能感知到；
     * 这也是不给它配成可环境变量覆盖的原因——调它需要连同压测一起做，
     * 留成代码里的显式决定比留成一个容易被误改的开关更稳。
     */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * 对明文密码进行加密
     *
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
