package com.shanzhu.music.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 文件路径工具类
 */
@Slf4j
public class PathUtils {

    public static String getClassLoadRootPath() {
        String path = "";

        try {
            String prePath =
                    URLDecoder.decode(PathUtils.class.getClassLoader().getResource("").getPath(), "utf-8").replace(
                    "/target/classes", "");
            String osName = System.getProperty("os.name");
            if (osName.toLowerCase().startsWith("mac")) {
                // 苹果
                path = prePath.substring(0, prePath.length() - 1);
            } else if (osName.toLowerCase().startsWith("windows")) {
                // windows
                path = prePath.substring(1, prePath.length() - 1);
            } else if (osName.toLowerCase().startsWith("linux") || osName.toLowerCase().startsWith("unix")) {
                // unix or linux
                path = prePath.substring(0, prePath.length() - 1);
            } else {
                path = prePath.substring(1, prePath.length() - 1);
            }
        } catch (UnsupportedEncodingException e) {
            log.error("解析类加载路径失败，返回空路径", e);
        }
        return path;
    }

}
