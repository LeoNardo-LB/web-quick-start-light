package org.smm.archetype.component.oss;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.dto.FileMetadata;
import org.smm.archetype.component.dto.OssUploadRequest;
import org.smm.archetype.component.dto.OssUploadResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.io.InputStream;
import java.util.List;

/**
 * OssComponent 抽象基类，使用 Template Method 模式。
 * <p>
 * 所有公开方法标记为 final，完成参数校验、异常处理与日志记录。
 * 子类实现 do* 扩展点完成具体存储操作。
 */
@Slf4j
public abstract class AbstractOssComponent implements OssComponent {

    @Override
    public final OssUploadResult upload(OssUploadRequest request) {
        if (request == null || request.fileName() == null || request.fileName().isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "fileName 不能为空");
        }
        if (request.inputStream() == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "inputStream 不能为空");
        }
        log.info("开始上传文件: {}", request.fileName());
        try {
            OssUploadResult result = doUpload(request);
            log.info("文件上传完成: {}, fileKey={}", request.fileName(), result.fileKey());
            return result;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传异常: {}", request.fileName(), e);
            throw new ClientException(CommonErrorCode.OSS_UPLOAD_FAILED, "文件上传失败: " + request.fileName(), e);
        }
    }

    @Override
    public final InputStream download(String fileKey) {
        validateFileKey(fileKey);
        log.info("开始下载文件: {}", fileKey);
        try {
            return doDownload(fileKey);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件下载异常: {}", fileKey, e);
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件下载失败: " + fileKey, e);
        }
    }

    @Override
    public final void delete(String fileKey) {
        validateFileKey(fileKey);
        log.info("开始删除文件: {}", fileKey);
        try {
            doDelete(fileKey);
            log.info("文件删除完成: {}", fileKey);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件删除异常: {}", fileKey, e);
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件删除失败: " + fileKey, e);
        }
    }

    @Override
    public final String generateUrl(String fileKey, long expireSeconds) {
        validateFileKey(fileKey);
        if (expireSeconds <= 0) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "过期时间必须为正数");
        }
        log.debug("生成文件 URL: fileKey={}, expireSeconds={}", fileKey, expireSeconds);
        return doGenerateUrl(fileKey, expireSeconds);
    }

    @Override
    public final List<FileMetadata> searchFiles(String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        log.debug("搜索文件: prefix={}", prefix);
        try {
            return doSearchFiles(prefix);
        } catch (Exception e) {
            log.error("搜索文件异常: prefix={}", prefix, e);
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件搜索失败: " + prefix, e);
        }
    }

    @Override
    public final boolean exists(String fileKey) {
        validateFileKey(fileKey);
        log.debug("检查文件存在: fileKey={}", fileKey);
        return doExists(fileKey);
    }

    @Override
    public final long getFileSize(String fileKey) {
        validateFileKey(fileKey);
        log.debug("获取文件大小: fileKey={}", fileKey);
        return doGetFileSize(fileKey);
    }

    // ==================== 参数校验 ====================

    private void validateFileKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "fileKey 不能为空");
        }
    }

    // ==================== 子类扩展点 ====================

    protected abstract OssUploadResult doUpload(OssUploadRequest request);

    protected abstract InputStream doDownload(String fileKey);

    protected abstract void doDelete(String fileKey);

    protected abstract String doGenerateUrl(String fileKey, long expireSeconds);

    protected abstract List<FileMetadata> doSearchFiles(String prefix);

    protected abstract boolean doExists(String fileKey);

    protected abstract long doGetFileSize(String fileKey);
}
