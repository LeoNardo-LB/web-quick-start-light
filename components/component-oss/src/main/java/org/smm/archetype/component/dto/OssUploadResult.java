package org.smm.archetype.component.dto;

public record OssUploadResult(
    boolean success,
    String fileKey,
    String url,
    String message
) {
    public static OssUploadResult success(String fileKey, String url) {
        return new OssUploadResult(true, fileKey, url, "上传成功");
    }

    public static OssUploadResult fail(String message) {
        return new OssUploadResult(false, null, null, message);
    }
}
