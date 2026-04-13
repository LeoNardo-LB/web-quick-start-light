package org.smm.archetype.client.dto;

/**
 * 文件元数据信息。
 */
public record FileMetadata(
        String fileKey,
        String fileName,
        long fileSize,
        String contentType,
        String url
) {}
