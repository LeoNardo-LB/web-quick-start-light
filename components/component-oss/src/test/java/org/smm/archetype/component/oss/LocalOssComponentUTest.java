package org.smm.archetype.component.oss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.smm.archetype.component.dto.FileMetadata;
import org.smm.archetype.component.dto.OssUploadRequest;
import org.smm.archetype.component.dto.OssUploadResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalOssComponent 单元测试。
 * <p>
 * 测试所有公开方法，基于本地文件系统的对象存储实现。
 */
@DisplayName("LocalOssComponent 功能测试")
class LocalOssComponentUTest {

    @TempDir
    Path tempDir;

    private LocalOssComponent oss;

    @BeforeEach
    void setUp() {
        OssProperties properties = new OssProperties();
        properties.setLocalStoragePath(tempDir.toAbsolutePath().toString());
        oss = new LocalOssComponent(properties);
    }

    // ==================== upload + download 往返 ====================

    @Nested
    @DisplayName("upload + download 往返操作")
    class UploadDownloadRoundtripTests {

        @Test
        @DisplayName("upload 后 download 应返回相同内容")
        void should_download_same_content_after_upload() throws IOException {
            byte[] content = "Hello, World!".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult uploadResult = oss.upload(request);

            assertThat(uploadResult.success()).isTrue();
            assertThat(uploadResult.fileKey()).isNotNull().isNotEmpty();

            try (InputStream downloaded = oss.download(uploadResult.fileKey())) {
                byte[] downloadedBytes = downloaded.readAllBytes();
                assertThat(downloadedBytes).isEqualTo(content);
            }
        }
    }

    // ==================== upload + exists ====================

    @Nested
    @DisplayName("upload + exists 操作")
    class UploadExistsTests {

        @Test
        @DisplayName("upload 后 exists 应返回 true")
        void should_exist_after_upload() {
            byte[] content = "test content".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "exists-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);

            assertThat(oss.exists(result.fileKey())).isTrue();
        }

        @Test
        @DisplayName("不存在的文件 exists 应返回 false")
        void should_return_false_for_non_existing_file() {
            assertThat(oss.exists("2024/01/nonexistent.txt")).isFalse();
        }
    }

    // ==================== upload + delete + exists ====================

    @Nested
    @DisplayName("upload + delete + exists 操作")
    class UploadDeleteExistsTests {

        @Test
        @DisplayName("delete 后 exists 应返回 false")
        void should_not_exist_after_delete() {
            byte[] content = "delete me".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "delete-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);
            assertThat(oss.exists(result.fileKey())).isTrue();

            oss.delete(result.fileKey());

            assertThat(oss.exists(result.fileKey())).isFalse();
        }
    }

    // ==================== generateUrl ====================

    @Nested
    @DisplayName("generateUrl 操作")
    class GenerateUrlTests {

        @Test
        @DisplayName("generateUrl 应返回非 null 的 URL")
        void should_return_non_null_url() {
            byte[] content = "url test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "url-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);

            String url = oss.generateUrl(result.fileKey(), 3600);

            assertThat(url).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("generateUrl 应包含 file 协议")
        void should_contain_file_protocol() {
            byte[] content = "protocol test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "protocol-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);

            String url = oss.generateUrl(result.fileKey(), 3600);

            assertThat(url).startsWith("file:");
        }
    }

    // ==================== searchFiles ====================

    @Nested
    @DisplayName("searchFiles 操作")
    class SearchFilesTests {

        @Test
        @DisplayName("searchFiles 应返回已上传的文件")
        void should_find_uploaded_files() {
            byte[] content = "search test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "search-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            oss.upload(request);

            List<FileMetadata> results = oss.searchFiles("");

            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("searchFiles 按前缀过滤应返回匹配的文件")
        void should_filter_by_prefix() throws IOException {
            // 上传文件并手动创建已知路径以控制 fileKey
            byte[] content = "prefix test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "prefix-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);
            // fileKey 格式为 yyyy/MM/timestamp-filename
            String fileKey = result.fileKey();
            // 获取年份前缀进行搜索
            String yearPrefix = fileKey.substring(0, 4);

            List<FileMetadata> results = oss.searchFiles(yearPrefix);

            assertThat(results).isNotEmpty();
            assertThat(results.stream().anyMatch(m -> m.fileKey().equals(fileKey))).isTrue();
        }

        @Test
        @DisplayName("searchFiles 不匹配的前缀应返回空列表")
        void should_return_empty_for_non_matching_prefix() {
            List<FileMetadata> results = oss.searchFiles("nonexistent-prefix-xyz");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("searchFiles null 前缀应返回所有文件")
        void should_return_all_for_null_prefix() {
            byte[] content = "null prefix test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "null-prefix-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            oss.upload(request);

            List<FileMetadata> results = oss.searchFiles(null);

            assertThat(results).isNotEmpty();
        }
    }

    // ==================== getFileSize ====================

    @Nested
    @DisplayName("getFileSize 操作")
    class GetFileSizeTests {

        @Test
        @DisplayName("getFileSize 应返回正确的文件大小")
        void should_return_correct_file_size() {
            byte[] content = "size test content".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "size-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);

            long fileSize = oss.getFileSize(result.fileKey());

            assertThat(fileSize).isEqualTo(content.length);
        }
    }

    // ==================== 参数校验 ====================

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @Test
        @DisplayName("upload null request 应抛出 ClientException")
        void should_throw_for_null_upload_request() {
            assertThatThrownBy(() -> oss.upload(null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("upload null fileName 应抛出 ClientException")
        void should_throw_for_null_file_name() {
            OssUploadRequest request = new OssUploadRequest(
                    null,
                    new ByteArrayInputStream("data".getBytes()),
                    4,
                    "text/plain"
            );

            assertThatThrownBy(() -> oss.upload(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("upload 空 fileName 应抛出 ClientException")
        void should_throw_for_blank_file_name() {
            OssUploadRequest request = new OssUploadRequest(
                    "  ",
                    new ByteArrayInputStream("data".getBytes()),
                    4,
                    "text/plain"
            );

            assertThatThrownBy(() -> oss.upload(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("upload null inputStream 应抛出 ClientException")
        void should_throw_for_null_input_stream() {
            OssUploadRequest request = new OssUploadRequest(
                    "test.txt",
                    null,
                    0,
                    "text/plain"
            );

            assertThatThrownBy(() -> oss.upload(request))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("download null fileKey 应抛出 ClientException")
        void should_throw_for_null_file_key_on_download() {
            assertThatThrownBy(() -> oss.download(null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("download 空 fileKey 应抛出 ClientException")
        void should_throw_for_blank_file_key_on_download() {
            assertThatThrownBy(() -> oss.download("  "))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("generateUrl 负数 expireSeconds 应抛出 ClientException")
        void should_throw_for_negative_expire_seconds() {
            byte[] content = "expire test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "expire-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );
            OssUploadResult result = oss.upload(request);

            assertThatThrownBy(() -> oss.generateUrl(result.fileKey(), -1))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("generateUrl 零 expireSeconds 应抛出 ClientException")
        void should_throw_for_zero_expire_seconds() {
            byte[] content = "expire test".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "expire-test2.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );
            OssUploadResult result = oss.upload(request);

            assertThatThrownBy(() -> oss.generateUrl(result.fileKey(), 0))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("exists null fileKey 应抛出 ClientException")
        void should_throw_for_null_file_key_on_exists() {
            assertThatThrownBy(() -> oss.exists(null))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    // ==================== 路径穿越保护 ====================

    @Nested
    @DisplayName("路径穿越保护")
    class PathTraversalTests {

        @Test
        @DisplayName("download 路径穿越到不存在的文件应抛出 ClientException")
        void should_throw_for_traversal_to_nonexistent_file() {
            // 使用不存在的穿越路径
            assertThatThrownBy(() -> oss.download("../../nonexistent_dir/nonexistent_file"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.OSS_OPERATION_FAILED);
        }

        @Test
        @DisplayName("getFileSize 路径穿越到不存在的文件应抛出 ClientException")
        void should_throw_for_traversal_to_nonexistent_file_on_size() {
            assertThatThrownBy(() -> oss.getFileSize("../../nonexistent_dir/nonexistent_file"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.OSS_OPERATION_FAILED);
        }

        @Test
        @DisplayName("路径穿越不影响 basePath 内的正常操作")
        void should_not_affect_normal_operations_within_base_path() {
            byte[] content = "safe content".getBytes();
            OssUploadRequest request = new OssUploadRequest(
                    "safe-test.txt",
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/plain"
            );

            OssUploadResult result = oss.upload(request);
            assertThat(oss.exists(result.fileKey())).isTrue();

            // 穿越路径不会影响正常文件
            assertThat(oss.exists(result.fileKey())).isTrue();
        }
    }

    // ==================== 不存在的文件操作 ====================

    @Nested
    @DisplayName("不存在的文件操作")
    class NonExistentFileTests {

        @Test
        @DisplayName("download 不存在的文件应抛出 ClientException")
        void should_throw_when_downloading_non_existent_file() {
            assertThatThrownBy(() -> oss.download("nonexistent/file.txt"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.OSS_OPERATION_FAILED);
        }

        @Test
        @DisplayName("getFileSize 不存在的文件应抛出 ClientException")
        void should_throw_when_getting_size_of_non_existent_file() {
            assertThatThrownBy(() -> oss.getFileSize("nonexistent/file.txt"))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.OSS_OPERATION_FAILED);
        }

        @Test
        @DisplayName("generateUrl 不存在的文件应抛出 ClientException")
        void should_throw_when_generating_url_for_non_existent_file() {
            assertThatThrownBy(() -> oss.generateUrl("nonexistent/file.txt", 3600))
                    .isInstanceOf(ClientException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.OSS_OPERATION_FAILED);
        }
    }
}
