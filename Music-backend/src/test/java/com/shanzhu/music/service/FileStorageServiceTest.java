package com.shanzhu.music.service;

import com.shanzhu.music.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传校验的失败文案与异常类型。
 *
 * 盯两件事：
 * 1) 文案。它是对外可见文本——调用方不再自己 catch，message 由
 *    GlobalExceptionHandler 放进 R.error 回到前端。一旦里面拼进服务器绝对路径、
 *    真实文件名或内部堆栈，就等于替外人画出目录结构。
 * 2) 异常类型必须是 BusinessException。这个类型在本项目里不是随便挑的：
 *    它的 handler 保持 HTTP 200、失败信息放在 code/message；
 *    而 IllegalArgumentException 的 handler 带 @ResponseStatus(400)，
 *    两套前端的 el-upload 只在 2xx 触发 :on-success，一旦变 4xx，
 *    "文件类型不支持"这类可解释失败就落到 :on-error，提示口径分裂。
 *    所以这条断言是在守住一个跨栈契约，不只是在测类型。
 *
 * mkdirs 失败那条分支（存储不可用）在单测里没法触发：它要求文件系统真的拒绝建目录，
 * 而测试只能控制自己传入的内容。它的修复靠评审保证 —— 路径只进 log.error，
 * 对外统一 BusinessException(500, "服务器存储不可用，请稍后重试")。
 */
class FileStorageServiceTest {

    private final FileStorageService service = new FileStorageService();

    /**
     * 任何对外文案里都不该出现的内部线索
     */
    private static final List<String> INTERNAL_MARKERS = List.of(
            "\\", "/", "C:", "E:", ".java", "target", "classLoader"
    );

    @Test
    @DisplayName("空文件被拒，文案不含路径")
    void rejectsEmptyFile() throws IOException {
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveImage(empty, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
    }

    @Test
    @DisplayName("超出大小上限，文案只说大小不说路径")
    void rejectsOversizedImage() throws IOException {
        byte[] big = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
        assertTrue(e.getMessage().contains("MB"), "超限文案应给出上限，实际: " + e.getMessage());
    }

    @Test
    @DisplayName("扩展名不在白名单，文案不含落盘细节")
    void rejectsUnknownExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "evil.exe",
                "application/octet-stream", new byte[]{1, 2, 3});
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
    }

    @Test
    @DisplayName("内容与扩展名不符时给出的文案不暴露检查逻辑")
    void rejectsContentMismatch() throws IOException {
        // 名字与 MIME 都报 png，内容是一段文本：文件头签名核对必须拦住它
        MockMultipartFile file = new MockMultipartFile("file", "fake.png",
                "image/png", "not really a png".getBytes());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
    }

    @Test
    @DisplayName("音频走同一套文案约束")
    void rejectsMismatchedAudio() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write("fake audio".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "fake.mp3", "audio/mpeg", body.toByteArray());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveAudio(file, "song"));
        assertMessageIsClientSafe(e.getMessage());
    }

    /**
     * 环境类失败用 code=500、校验类失败用默认的 code=400，两者都仍是 HTTP 200。
     * 这里能验到的是后者：入参不合法时 code 必须是 400，前端据此决定提示文案的强度。
     */
    @Test
    @DisplayName("校验类失败的错误码是 400")
    void validationFailureUsesClientCode() {
        MockMultipartFile file = new MockMultipartFile("file", "", "application/octet-stream", new byte[0]);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertEquals(400, e.getCode(), "客户端输入问题不该被标成服务端错误");
    }

    private static void assertMessageIsClientSafe(String message) {
        assertTrue(message != null && !message.isBlank(), "失败文案不应为空，否则前端只能显示兜底提示");
        for (String marker : INTERNAL_MARKERS) {
            assertTrue(!message.contains(marker),
                    "对外文案不应包含内部线索 [" + marker + "]，实际: " + message);
        }
    }
}
