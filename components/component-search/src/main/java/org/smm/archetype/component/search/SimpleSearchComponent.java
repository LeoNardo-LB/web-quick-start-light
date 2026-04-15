package org.smm.archetype.component.search;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.component.dto.SearchQuery;
import org.smm.archetype.component.dto.SearchResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的简单搜索组件实现。
 * <p>
 * 使用 ConcurrentHashMap 存储文档，支持全文关键字匹配、分页、聚合等功能。
 * 适用于开发环境和测试场景。
 */
@Slf4j
public class SimpleSearchComponent extends AbstractSearchComponent {

    /** 文档存储：key = "indexName:docId" */
    private final ConcurrentHashMap<String, Map<String, Object>> documents = new ConcurrentHashMap<>();

    /** 索引存在标记 */
    private final Set<String> indices = ConcurrentHashMap.newKeySet();

    @Override
    @SuppressWarnings("unchecked")
    protected void doIndex(String indexName, String id, Object document) {
        Map<String, Object> docMap;
        if (document instanceof Map) {
            docMap = new ConcurrentHashMap<>((Map<String, Object>) document);
        } else {
            docMap = JSON.parseObject(JSON.toJSONString(document), Map.class);
        }
        docMap.put("_id", id);
        documents.put(indexName + ":" + id, docMap);
        indices.add(indexName);
        log.debug("索引文档成功: indexName={}, id={}", indexName, id);
    }

    @Override
    protected void doBulkIndex(String indexName, List<Map<String, Object>> docs) {
        for (Map<String, Object> doc : docs) {
            Object idObj = doc.get("id");
            if (idObj == null) {
                idObj = doc.get("_id");
            }
            if (idObj == null) {
                idObj = UUID.randomUUID().toString();
            }
            String id = idObj.toString();
            Map<String, Object> docCopy = new ConcurrentHashMap<>(doc);
            docCopy.put("_id", id);
            documents.put(indexName + ":" + id, docCopy);
        }
        indices.add(indexName);
        log.debug("批量索引成功: indexName={}, count={}", indexName, docs.size());
    }

    @Override
    protected void doDelete(String indexName, String id) {
        documents.remove(indexName + ":" + id);
        log.debug("删除文档成功: indexName={}, id={}", indexName, id);
    }

    @Override
    protected Map<String, Object> doGet(String indexName, String id) {
        Map<String, Object> doc = documents.get(indexName + ":" + id);
        if (doc == null) {
            return null;
        }
        // 返回副本，移除内部字段
        Map<String, Object> result = new LinkedHashMap<>(doc);
        result.remove("_id");
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected SearchResult doSearch(SearchQuery query) {
        String prefix = query.indexName() + ":";
        String keyword = query.keyword();

        List<Map<String, Object>> matched = documents.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .filter(entry -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    return entry.getValue().values().stream()
                            .anyMatch(v -> v != null && v.toString().contains(keyword));
                })
                .map(entry -> {
                    Map<String, Object> result = new LinkedHashMap<>(entry.getValue());
                    result.remove("_id");
                    return result;
                })
                .collect(Collectors.toList());

        long total = matched.size();
        int fromIndex = Math.max(0, (query.pageNo() - 1) * query.pageSize());
        int toIndex = Math.min(fromIndex + query.pageSize(), matched.size());
        List<Map<String, Object>> pageRecords = fromIndex < matched.size()
                ? matched.subList(fromIndex, toIndex)
                : new ArrayList<>();

        return new SearchResult(total, pageRecords, query.pageNo(), query.pageSize());
    }

    @Override
    protected Map<String, Long> doAggregate(String indexName, String fieldName, SearchQuery query) {
        String prefix = indexName + ":";
        String keyword = query != null ? query.keyword() : null;

        Map<String, Long> result = new LinkedHashMap<>();
        documents.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .filter(entry -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    return entry.getValue().values().stream()
                            .anyMatch(v -> v != null && v.toString().contains(keyword));
                })
                .forEach(entry -> {
                    Object fieldValue = entry.getValue().get(fieldName);
                    String key = fieldValue != null ? fieldValue.toString() : "null";
                    result.merge(key, 1L, Long::sum);
                });

        return result;
    }

    @Override
    protected boolean doExists(String indexName, String id) {
        return documents.containsKey(indexName + ":" + id);
    }

    @Override
    protected boolean doExistsIndex(String indexName) {
        return indices.contains(indexName);
    }

    @Override
    protected boolean doCreateIndex(String indexName) {
        indices.add(indexName);
        return true;
    }

    @Override
    protected boolean doDeleteIndex(String indexName) {
        String prefix = indexName + ":";
        documents.keySet().removeIf(key -> key.startsWith(prefix));
        indices.remove(indexName);
        return true;
    }

    @Override
    protected void doRefresh(String indexName) {
        // 内存实现无需刷新
        log.debug("刷新索引（内存实现无需操作）: indexName={}", indexName);
    }

    @Override
    protected long doCount(String indexName) {
        String prefix = indexName + ":";
        return documents.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .count();
    }

    @Override
    protected void doBulkDelete(String indexName, List<String> ids) {
        for (String id : ids) {
            documents.remove(indexName + ":" + id);
        }
        log.debug("批量删除成功: indexName={}, count={}", indexName, ids.size());
    }

    @Override
    protected void doUpdate(String indexName, String id, Map<String, Object> document) {
        String key = indexName + ":" + id;
        Map<String, Object> existing = documents.get(key);
        if (existing == null) {
            return;
        }
        existing.putAll(document);
        existing.put("_id", id);
        log.debug("更新文档成功: indexName={}, id={}", indexName, id);
    }
}
