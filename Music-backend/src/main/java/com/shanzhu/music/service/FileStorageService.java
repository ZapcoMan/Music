package com.shanzhu.music.service;

import com.shanzhu.music.exception.BusinessException;
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

    /**
     * 校验并落盘，返回可直接入库的相对路径。
     *
     * 失败一律抛 BusinessException，由 GlobalExceptionHandler 映射成
     * HTTP 200 + success=false + 可读 message。这里刻意不用
     * IllegalArgumentException（那个 handler 带 @ResponseStatus(400)）：
     * 两套前端的 el-upload 只在 2xx 触发 :on-success，把"文件类型不支持"这类
     * 可解释失败变成 4xx 会把它推到 :on-error，提示口径分裂。
     * 换成 BusinessException 之后，六个上传端点里那九处
     * try { save } catch { return R.error(e.getMessage()) } 才第一次有可能消失。
     *
     * @return 相对路径，例如 /img/songPic/1715868053-ab12cd34.jpg
     */
    private String store(MultipartFile file,
                         String subDir, Set<String> allowedExtensions, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件超过大小上限 " + (maxBytes / 1024 / 1024) + "MB");
        }

        String originalName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), ""));
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("不支持的文件类型：" + extension);
        }
        String normalizedExtension = extension.toLowerCase(Locale.ROOT);

        // 扩展名是客户端可以随意改的字段：把脚本改名成 .jpg 就能过上面那道白名单，
        // 落盘之后它就是个"图片"，由静态目录直出。要求内容与声明格式一致。
        // 这里读一次流做判定，之后 transferTo 仍会重新打开自己的流，互不影响。
        byte[] head = new byte[16];
        int headLength;
        try (InputStream in = file.getInputStream()) {
            headLength = in.read(head);
        } catch (IOException e) {
            // 读流失败是环境或客户端问题，不是"文件内容不合法"，但仍然要给可懂的文案
            log.warn("读取上传文件流失败 - name={}", originalName, e);
            throw new BusinessException("文件读取失败，请重试");
        }
        if (headLength <= 0) {
            throw new BusinessException("文件内容无法识别（空或已损坏）");
        }
        if (!matchesContent(normalizedExtension, head)) {
            throw new BusinessException("文件内容与扩展名不符：" + normalizedExtension);
        }

        // 落盘文件名完全由服务端生成，不沿用客户端提供的名字，从根源上排除路径穿越
        String safeName = System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8)
                + "." + normalizedExtension;

        String dirPath = PathUtils.getClassLoadRootPath() + "/" + subDir + "/";
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            // 绝对路径只进日志：这段文字会随响应体回到浏览器，等于替外人画出服务端目录结构
            log.error("创建上传目录失败 - dir={}", dirPath);
            throw new BusinessException(500, "服务器存储不可用，请稍后重试");
        }

        File dest = new File(dir, safeName);
        // 纵深防御：即使命名策略被改动，也要求最终路径落在预期目录内。
        // getCanonicalPath 自己也抛 IOException（要问文件系统真实路径），所以它必须在 try 里——
        // 本类的方法签名不声明 checked 异常，失败一律转成 BusinessException。
        try {
            if (!dest.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
                // 进到这一句说明 safeName 的生成规则被改坏了，属于服务端缺陷而非用户输入问题：
                // 记 error 留证据，对外只说"保存失败"，不解释内部命名策略
                log.error("落盘路径越出预期目录 - dir={}", dirPath);
                throw new BusinessException(500, "文件保存失败，请重试");
            }

            file.transferTo(dest);
        } catch (IOException | IllegalStateException e) {
            // IOException 来自规范化路径或写入本身；IllegalStateException 是
            // transferTo 在目标已存在（File exists）或临时文件已被清理时抛的。
            // 两者都属于服务端环境问题，不该冒成"系统异常"，也不该带出路径
            log.error("写入上传文件失败 - path={}", dest.getAbsolutePath(), e);
            throw new BusinessException(500, "文件保存失败，请重试");
        }
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
    public String saveImage(MultipartFile file, String subDir) {
        return store(file, subDir, IMAGE_EXTENSIONS, MAX_IMAGE_BYTES);
    }

    /**
     * 保存音频，返回入库的相对路径
     *
     * @param file   上传文件
     * @param subDir 相对根目录的子目录名，如 song
     */
    public String saveAudio(MultipartFile file, String subDir) {
        return store(file, subDir, AUDIO_EXTENSIONS, MAX_AUDIO_BYTES);
    }
}
