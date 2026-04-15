package org.smm.archetype.component.dto;

import java.util.List;
import java.util.Map;

public record SearchResult(
    long total,
    List<Map<String, Object>> records,
    int pageNo,
    int pageSize
) {}
