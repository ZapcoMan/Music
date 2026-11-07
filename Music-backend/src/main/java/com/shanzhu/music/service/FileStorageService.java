package com.shanzhu.music.service;

import com.shanzhu.music.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传落盘服务。
 *
 * 此前六处上传各自重复"拼路径 -> mkdir -> transferTo"，并且没有任何扩展名、
 * 大小与文件名安全校验：文件名直接由 getOriginalFilename() 拼接而来，
 * 原始名中携带路径分隔符或 ../ 时存在写到预期目录之外的风险；
 * 上传上限又被配到 1024MB，实际等同不设限。
 * 这里把校验、命名与落盘集中到一处，调用方只关心业务字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    /** 允许的图片类型 */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    /** 允许的音频类型 */
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "flac", "aac", "ogg", "m4a");

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_AUDIO_BYTES = 100L * 1024 * 1024;

    private String store(MultipartFile file,
                         String subDir, Set<String> allowedExtensions, long maxBytes) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("文件超过大小上限 " + (maxBytes / 1024 / 1024) + "MB");
        }

        String originalName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), ""));
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("不支持的文件类型：" + extension);
        }

        // 落盘文件名完全由服务端生成，不沿用客户端提供的名字，从根源上排除路径穿越
        String safeName = System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8)
                + "." + extension.toLowerCase(Locale.ROOT);

        String dirPath = PathUtils.getClassLoadRootPath() + "/" + subDir + "/";
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("创建上传目录失败：" + dirPath);
        }

        File dest = new File(dir, safeName);
        // 纵深防御：即使命名策略被改动，也要求最终路径落在预期目录内
        if (!dest.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
            throw new IllegalArgumentException("非法的目标文件路径");
        }

        file.transferTo(dest);
        String relativePath = "/" + subDir + "/" + safeName;
        log.info("文件上传成功: {} ({} bytes)", relativePath, file.getSize());
        return relativePath;
    }

    /**
     * 保存图片，返回入库的相对路径
     *
     * @param file   上传文件
     * @param subDir 相对根目录的子目录名，如 img/songPic
     */
    public String saveImage(MultipartFile file, String subDir) throws IOException {
        return store(file, subDir, IMAGE_EXTENSIONS, MAX_IMAGE_BYTES);
    }

    /**
     * 保存音频，返回入库的相对路径
     *
     * @param file   上传文件
     * @param subDir 相对根目录的子目录名，如 song
     */
    public String saveAudio(MultipartFile file, String subDir) throws IOException {
        return store(file, subDir, AUDIO_EXTENSIONS, MAX_AUDIO_BYTES);
    }
}
