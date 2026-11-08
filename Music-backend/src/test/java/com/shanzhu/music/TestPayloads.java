package com.shanzhu.music;

/**
 * 上传用例所需的最小合法文件头。
 *
 * FileStorageService 现在会把内容与扩展名对照，早先那些
 * "x".getBytes()、new byte[]{1} 之类的占位内容会被当场拒掉，
 * 用例失败在"文件内容与扩展名不符"上，看起来反倒像校验有 bug。
 * 签名集中在这里，避免每个测试类各抄一遍——抄错一个字节就是同样的误导。
 */
public final class TestPayloads {

    /**
     * ID3v2 标签头，补零到 16 字节以满足签名判定的读取长度
     */
    public static final byte[] MP3 = padded(new byte[]{'I', 'D', '3', 0x04, 0x00, 0x00});

    /**
     * PNG 的 8 字节签名，后接占位长度字段
     */
    public static final byte[] PNG = padded(
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    /**
     * JPEG 的 SOI 标记
     */
    public static final byte[] JPEG = padded(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

    private TestPayloads() {
    }

    private static byte[] padded(byte[] signature) {
        byte[] out = new byte[16];
        System.arraycopy(signature, 0, out, 0, signature.length);
        return out;
    }
}
