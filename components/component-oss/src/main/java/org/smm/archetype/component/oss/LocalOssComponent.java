package org.smm.archetype.component.oss;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.dto.FileMetadata;
import org.smm.archetype.component.dto.OssUploadRequest;
import org.smm.archetype.component.dto.OssUploadResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 基于本地文件系统的对象存储实现。
 * <p>
 * 使用日期分层目录结构（yyyy/MM），NIO 零拷贝文件操作。
 */
@Slf4j
public class LocalOssComponent extends AbstractOssComponent {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path basePath;

    public LocalOssComponent(OssProperties properties) {
        this.basePath = Paths.get(properties.getLocalStoragePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
            log.info("本地存储根目录: {}", this.basePath);
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "无法创建存储目录: " + this.basePath, e);
        }
    }

    @Override
    protected OssUploadResult doUpload(OssUploadRequest request) {
        String dateDir = LocalDate.now().format(DATE_FORMATTER);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String storedFileName = timestamp + "-" + request.fileName();

        Path dirPath = basePath.resolve(dateDir);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_UPLOAD_FAILED, "无法创建日期目录: " + dirPath, e);
        }

        Path targetPath = dirPath.resolve(storedFileName);
        try (InputStream is = request.inputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_UPLOAD_FAILED, "文件写入失败: " + request.fileName(), e);
        }

        String fileKey = dateDir + "/" + storedFileName;
        return OssUploadResult.success(fileKey, targetPath.toUri().toString());
    }

    @Override
    protected InputStream doDownload(String fileKey) {
        Path filePath = resolveAndValidate(fileKey);
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件读取失败: " + fileKey, e);
        }
    }

    @Override
    protected void doDelete(String fileKey) {
        Path filePath = resolveAndValidate(fileKey);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件删除失败: " + fileKey, e);
        }
    }

    @Override
    protected String doGenerateUrl(String fileKey, long expireSeconds) {
        Path filePath = resolveAndValidate(fileKey);
        return filePath.toUri().toString();
    }

    @Override
    protected List<FileMetadata> doSearchFiles(String prefix) {
        List<FileMetadata> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(basePath)) {
            walk.filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String relativePath = basePath.relativize(filePath).toString();
                        if (relativePath.startsWith(prefix)) {
                            try {
                                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                                result.add(new FileMetadata(
                                        relativePath,
                                        filePath.getFileName().toString(),
                                        attrs.size(),
                                        probeContentType(filePath),
                                        filePath.toUri().toString()
                                ));
                            } catch (IOException e) {
                                log.warn("读取文件属性失败: {}", filePath, e);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件搜索失败: " + prefix, e);
        }
        return result;
    }

    @Override
    protected boolean doExists(String fileKey) {
        Path filePath = basePath.resolve(fileKey).normalize();
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    @Override
    protected long doGetFileSize(String fileKey) {
        Path filePath = resolveAndValidate(fileKey);
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "获取文件大小失败: " + fileKey, e);
        }
    }

    // ==================== 内部方法 ====================

    private Path resolveAndValidate(String fileKey) {
        Path filePath = basePath.resolve(fileKey).normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ClientException(CommonErrorCode.OSS_OPERATION_FAILED, "文件不存在: " + fileKey);
        }
        return filePath;
    }

    private String probeContentType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
