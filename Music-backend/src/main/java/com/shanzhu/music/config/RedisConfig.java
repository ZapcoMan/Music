package com.shanzhu.music.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {

    /**
     * 缓存值序列化用的 ObjectMapper。
     *
     * 此前第二参数用 LaissezFaireSubTypeValidator，等于允许反序列化任意类名：
     * 写入 Redis 的 JSON 里 "@class" 是不可信输入，一旦Redis可被低权限方写入
     * （共享实例、被攻陷的其它服务、调试接口），就构成反序列化 gadget 链的入口。
     * 现改为白名单，只放行本项目与缓存里实际会出现的类型。
     *
     * 抽成方法而非内联在 @Bean 里，是为了让校验探针能针对同一份生产配置做往返，
     * 而不是在探针里另抄一遍逻辑得出与线上不同的结论。
     * 白名单一旦收紧，漏放某类会使该类缓存读取抛 InvalidTypeIdException，
     * 因此新增可缓存类型时必须同步这里。
     */
    static ObjectMapper buildCacheObjectMapper() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                // 本项目实体、VO 与 CacheEntry 包装
                .allowIfSubType("com.shanzhu.music.")
                // 集合与 java.util.Date（实体里的时间字段实际类型）
                .allowIfSubType("java.util.")
                // java.math.BigDecimal 等数值
                .allowIfSubType("java.math.")
                // MyBatis 把 DATETIME 映射到实体的 java.util.Date 字段时，
                // 运行时实际类型是 java.sql.Timestamp，写入 JSON 的是其自身类名
                .allowIfSubType("java.sql.")
                // hutool 的 DateTime 继承 java.util.Date，序列化时写入的是其自身类名
                .allowIfSubType("cn.hutool.")
                // PageHelper 的 PageInfo 会被整体缓存
                .allowIfSubType("com.github.pagehelper.")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // value序列化方式采用jackson
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(buildCacheObjectMapper(), Object.class);

        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(serializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }
}
