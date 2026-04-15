package org.smm.archetype.component.dto;

public record SearchQuery(
    String keyword,
    String indexName,
    int pageNo,
    int pageSize
) {}
