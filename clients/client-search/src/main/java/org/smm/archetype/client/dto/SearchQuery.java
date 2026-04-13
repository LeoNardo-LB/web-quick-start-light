package org.smm.archetype.client.dto;

public record SearchQuery(
    String keyword,
    String indexName,
    int pageNo,
    int pageSize
) {}
