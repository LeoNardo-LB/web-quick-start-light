package org.smm.archetype.client.dto;

import java.io.InputStream;

public record OssUploadRequest(
    String fileName,
    InputStream inputStream,
    long contentLength,
    String contentType
) {}
