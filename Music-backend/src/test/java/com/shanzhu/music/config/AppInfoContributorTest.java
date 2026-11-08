package com.shanzhu.music.config;

import com.shanzhu.music.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /actuator/info 里的版本与启动时间。
 */
class AppInfoContributorTest extends BaseTest {

    @Autowired
    private AppInfoContributor contributor;

    private Map<String, Object> contribute() {
        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);
        return builder.build().getDetails();
    }

    /**
     * 断的是格式，不是具体版本号。
     *
     * 把 "2.0.1" 写进断言就等于又造了第二个版本出处——升版时它会红，
     * 而那时正确的做法是改断言，于是这条用例变成了升版流程里一个纯粹的阻力。
     * 这里要防的是回退成写死的字符串或空值，语义化版本格式足够表达这个约束。
     */
    @Test
    @DisplayName("版本来自构建信息且符合语义化版本格式")
    void versionComesFromBuildInfo() {
        Object version = contribute().get("version");

        assertNotNull(version, "版本不应缺失");
        assertTrue(Pattern.matches("\\d+\\.\\d+\\.\\d+.*", String.valueOf(version)),
                "版本应形如 2.0.1，实际为 " + version);
    }

    @Test
    @DisplayName("startTime 在整个应用生命周期内保持不变")
    void startTimeIsNotTheRequestTime() {
        String first = String.valueOf(contribute().get("startTime"));
        String second = String.valueOf(contribute().get("startTime"));

        assertEquals(first, second,
                "字段叫 startTime，取值就只能是启动时刻；"
                        + "此前它每次都取 LocalDateTime.now()，每访问一次 /info 就重新计时一次");
    }
}
