package com.shanzhu.music.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传校验的失败文案。
 *
 * 这组用例盯的不是"能不能拒掉坏文件"，而是拒掉时说了什么：
 * Controller 会把 IllegalArgumentException / IllegalStateException 的 message
 * 原样塞进 R.error 返回前端（本项目 el-upload 只在 2xx 走 on-success，
 * 把这类可解释失败改成 4xx 会让前端提示口径分裂，所以刻意保留 200 + success=false）。
 * 也就是说，message 是对外可见文本 —— 一旦它拼进服务器绝对路径、真实文件名或
 * 内部堆栈，就等于替外部画出目录结构。
 *
 * mkdirs 失败那条分支（存储不可用）在单测里没法触发：它要求文件系统真的拒绝建目录，
 * 而测试只能控制自己传入的内容。它的修复靠代码评审保证 —— 路径只进 log.error，
 * 对外统一成"服务器存储不可用，请稍后重试"。
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
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.saveImage(empty, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
    }

    @Test
    @DisplayName("超出大小上限，文案只说大小不说路径")
    void rejectsOversizedImage() throws IOException {
        byte[] big = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
        assertTrue(e.getMessage().contains("MB"), "超限文案应给出上限，实际: " + e.getMessage());
    }

    @Test
    @DisplayName("扩展名不在白名单，文案不含落盘细节")
    void rejectsUnknownExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "evil.exe",
                "application/octet-stream", new byte[]{1, 2, 3});
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
    }

    @Test
    @DisplayName("内容与扩展名不符时给出的文案不暴露检查逻辑")
    void rejectsContentMismatch() throws IOException {
        // 名字与 MIME 都报 png，内容是一段文本：文件头签名核对必须拦住它
        MockMultipartFile file = new MockMultipartFile("file", "fake.png",
                "image/png", "not really a png".getBytes());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.saveImage(file, "img/songPic"));
        assertMessageIsClientSafe(e.getMessage());
    }

    @Test
    @DisplayName("音频走同一套文案约束")
    void rejectsMismatchedAudio() throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write("fake audio".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "fake.mp3", "audio/mpeg", body.toByteArray());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.saveAudio(file, "song"));
        assertMessageIsClientSafe(e.getMessage());
    }

    private static void assertMessageIsClientSafe(String message) {
        assertTrue(message != null && !message.isBlank(), "失败文案不应为空，否则前端只能显示兜底提示");
        for (String marker : INTERNAL_MARKERS) {
            assertTrue(!message.contains(marker),
                    "对外文案不应包含内部线索 [" + marker + "]，实际: " + message);
        }
    }
}
