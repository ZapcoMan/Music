package com.shanzhu.music.service;

import com.shanzhu.music.utils.PathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);

        // 扩展名是客户端可以随意改的字段：把脚本改名成 .jpg 就能过上面那道白名单，
        // 落盘之后它就是个"图片"，由静态目录直出。要求内容与声明格式一致。
        // 这里读一次流做判定，之后 transferTo 仍会重新打开自己的流，互不影响。
        byte[] head = new byte[16];
        int headLength;
        try (InputStream in = file.getInputStream()) {
            headLength = in.read(head);
        }
        if (headLength <= 0) {
            throw new IllegalArgumentException("文件内容无法识别（空或已损坏）");
        }
        if (!matchesContent(normalizedExtension, head)) {
            throw new IllegalArgumentException("文件内容与扩展名不符：" + normalizedExtension);
        }

        // 落盘文件名完全由服务端生成，不沿用客户端提供的名字，从根源上排除路径穿越
        String safeName = System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8)
                + "." + normalizedExtension;

        String dirPath = PathUtils.getClassLoadRootPath() + "/" + subDir + "/";
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            // 绝对路径只进日志：调用方（Controller 的 R.error(e.getMessage())）会把这段文字
            // 原样发到前端，等于替外人画出服务端目录结构
            log.error("创建上传目录失败 - dir={}", dirPath);
            throw new IllegalStateException("服务器存储不可用，请稍后重试");
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
     * 文件头签名核对。只列本项目白名单内的格式；
     * 新增扩展名时必须同时在这里加一条，否则 matchesContent 会走 default 直接拒绝。
     */
    private static boolean matchesContent(String extension, byte[] head) {
        switch (extension) {
            case "jpg":
            case "jpeg":
                return signatureMatches(head, 0, 0xFF, 0xD8, 0xFF);
            case "png":
                return signatureMatches(head, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif":
                return signatureMatches(head, 0, 'G', 'I', 'F', '8');
            case "webp":
                return signatureMatches(head, 0, 'R', 'I', 'F', 'F')
                        && signatureMatches(head, 8, 'W', 'E', 'B', 'P');
            case "mp3":
                // ID3v2 标签头，或裸的 MPEG 帧同步：首字节 0xFF，次字节高 3 位全为 1
                // （0xE0 是掩码而不是取值，实际可能是 0xE0–0xFF 之间的任一同步字）
                return signatureMatches(head, 0, 'I', 'D', '3')
                        || (head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xE0) == 0xE0);
            case "wav":
                return signatureMatches(head, 0, 'R', 'I', 'F', 'F')
                        && signatureMatches(head, 8, 'W', 'A', 'V', 'E');
            case "flac":
                return signatureMatches(head, 0, 'f', 'L', 'a', 'C');
            case "ogg":
                return signatureMatches(head, 0, 'O', 'g', 'g', 'S');
            case "aac":
                // ADTS 帧同步：0xFF + 次字节 0xF_ （1 位版本 + 4 位 stream 标志，掩码 0xF6）
                return head.length >= 2
                        && (head[0] & 0xFF) == 0xFF
                        && (head[1] & 0xF6) == 0xF0;
            case "m4a":
                // ISO BMFF：偏移 4 起是 "ftyp"
                return signatureMatches(head, 4, 'f', 't', 'y', 'p');
            default:
                return false;
        }
    }

    /**
     * 按偏移比较签名；长度不足一律视为不匹配，不抛越界。
     */
    private static boolean signatureMatches(byte[] head, int offset, int... signature) {
        if (head == null || head.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((head[offset + i] & 0xFF) != (signature[i] & 0xFF)) {
                return false;
            }
        }
        return true;
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
