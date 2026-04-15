package org.smm.archetype.component.oss;

import org.smm.archetype.component.dto.FileMetadata;
import org.smm.archetype.component.dto.OssUploadRequest;
import org.smm.archetype.component.dto.OssUploadResult;

import java.io.InputStream;
import java.util.List;

/**
 * 对象存储客户端接口。
 * <p>
 * 提供文件上传、下载、删除、URL 生成、文件搜索、存在性检查和文件大小查询等功能。
 */
public interface OssComponent {

    /**
     * 上传文件
     *
     * @param request 上传请求
     * @return 上传结果
     */
    OssUploadResult upload(OssUploadRequest request);

    /**
     * 下载文件
     *
     * @param fileKey 文件键
     * @return 文件输入流
     */
    InputStream download(String fileKey);

    /**
     * 删除文件
     *
     * @param fileKey 文件键
     */
    void delete(String fileKey);

    /**
     * 生成文件访问 URL
     *
     * @param fileKey      文件键
     * @param expireSeconds 过期时间（秒）
     * @return 访问 URL
     */
    String generateUrl(String fileKey, long expireSeconds);

    /**
     * 搜索文件
     *
     * @param prefix 文件键前缀
     * @return 文件元数据列表
     */
    List<FileMetadata> searchFiles(String prefix);

    /**
     * 检查文件是否存在
     *
     * @param fileKey 文件键
     * @return 存在返回 true
     */
    boolean exists(String fileKey);

    /**
     * 获取文件大小
     *
     * @param fileKey 文件键
     * @return 文件大小（字节），不存在返回 -1
     */
    long getFileSize(String fileKey);
}
