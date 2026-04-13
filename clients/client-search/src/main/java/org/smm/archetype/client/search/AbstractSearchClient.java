package org.smm.archetype.client.search;

import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.client.dto.SearchQuery;
import org.smm.archetype.client.dto.SearchResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.util.List;
import java.util.Map;

/**
 * SearchClient 抽象基类，使用 Template Method 模式。
 * <p>
 * 所有公开方法标记为 final，完成参数校验、异常处理与日志记录。
 * 子类实现 do* 扩展点完成具体搜索操作。
 */
@Slf4j
public abstract class AbstractSearchClient implements SearchClient {

    @Override
    public final void index(String indexName, String id, Object document) {
        validateIndexParams(indexName, id, document);
        log.debug("Index: indexName={}, id={}", indexName, id);
        try {
            doIndex(indexName, id, document);
        } catch (Exception e) {
            log.error("Index 异常: indexName={}, id={}", indexName, id, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "索引文档失败: " + indexName, e);
        }
    }

    @Override
    public final void bulkIndex(String indexName, List<Map<String, Object>> documents) {
        validateIndexName(indexName);
        if (documents == null || documents.isEmpty()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "documents 不能为空");
        }
        log.debug("BulkIndex: indexName={}, count={}", indexName, documents.size());
        try {
            doBulkIndex(indexName, documents);
        } catch (Exception e) {
            log.error("BulkIndex 异常: indexName={}", indexName, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "批量索引失败: " + indexName, e);
        }
    }

    @Override
    public final void delete(String indexName, String id) {
        validateIndexNameAndId(indexName, id);
        log.debug("Delete: indexName={}, id={}", indexName, id);
        try {
            doDelete(indexName, id);
        } catch (Exception e) {
            log.error("Delete 异常: indexName={}, id={}", indexName, id, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "删除文档失败: " + indexName, e);
        }
    }

    @Override
    public final Map<String, Object> get(String indexName, String id) {
        validateIndexNameAndId(indexName, id);
        log.debug("Get: indexName={}, id={}", indexName, id);
        try {
            return doGet(indexName, id);
        } catch (Exception e) {
            log.error("Get 异常: indexName={}, id={}", indexName, id, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "获取文档失败: " + indexName, e);
        }
    }

    @Override
    public final SearchResult search(SearchQuery query) {
        if (query == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "search query must not be null");
        }
        validateIndexName(query.indexName());
        log.debug("Search: indexName={}, keyword={}", query.indexName(), query.keyword());
        try {
            return doSearch(query);
        } catch (Exception e) {
            log.error("Search 异常: indexName={}", query.indexName(), e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "搜索失败: " + query.indexName(), e);
        }
    }

    @Override
    public final Map<String, Long> aggregate(String indexName, String fieldName, SearchQuery query) {
        validateIndexName(indexName);
        if (fieldName == null || fieldName.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "fieldName 不能为空");
        }
        log.debug("Aggregate: indexName={}, field={}", indexName, fieldName);
        try {
            return doAggregate(indexName, fieldName, query);
        } catch (Exception e) {
            log.error("Aggregate 异常: indexName={}, field={}", indexName, fieldName, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "聚合查询失败: " + indexName, e);
        }
    }

    @Override
    public final boolean exists(String indexName, String id) {
        validateIndexNameAndId(indexName, id);
        log.debug("Exists: indexName={}, id={}", indexName, id);
        return doExists(indexName, id);
    }

    @Override
    public final boolean existsIndex(String indexName) {
        validateIndexName(indexName);
        log.debug("ExistsIndex: indexName={}", indexName);
        return doExistsIndex(indexName);
    }

    @Override
    public final boolean createIndex(String indexName) {
        validateIndexName(indexName);
        log.debug("CreateIndex: indexName={}", indexName);
        return doCreateIndex(indexName);
    }

    @Override
    public final boolean deleteIndex(String indexName) {
        validateIndexName(indexName);
        log.debug("DeleteIndex: indexName={}", indexName);
        return doDeleteIndex(indexName);
    }

    @Override
    public final void refresh(String indexName) {
        validateIndexName(indexName);
        log.debug("Refresh: indexName={}", indexName);
        doRefresh(indexName);
    }

    @Override
    public final long count(String indexName) {
        validateIndexName(indexName);
        log.debug("Count: indexName={}", indexName);
        return doCount(indexName);
    }

    @Override
    public final void bulkDelete(String indexName, List<String> ids) {
        validateIndexName(indexName);
        if (ids == null || ids.isEmpty()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "ids 不能为空");
        }
        log.debug("BulkDelete: indexName={}, count={}", indexName, ids.size());
        try {
            doBulkDelete(indexName, ids);
        } catch (Exception e) {
            log.error("BulkDelete 异常: indexName={}", indexName, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "批量删除失败: " + indexName, e);
        }
    }

    @Override
    public final void update(String indexName, String id, Map<String, Object> document) {
        validateIndexNameAndId(indexName, id);
        if (document == null || document.isEmpty()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "document 不能为空");
        }
        log.debug("Update: indexName={}, id={}", indexName, id);
        try {
            doUpdate(indexName, id, document);
        } catch (Exception e) {
            log.error("Update 异常: indexName={}, id={}", indexName, id, e);
            throw new ClientException(CommonErrorCode.SEARCH_OPERATION_FAILED, "更新文档失败: " + indexName, e);
        }
    }

    @Override
    public final SearchResult search(String indexName, String keyword, int pageNo, int pageSize) {
        validateIndexName(indexName);
        SearchQuery query = new SearchQuery(keyword, indexName, pageNo, pageSize);
        return search(query);
    }

    // ==================== 参数校验 ====================

    private void validateIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "indexName must not be null or blank");
        }
    }

    private void validateIndexNameAndId(String indexName, String id) {
        validateIndexName(indexName);
        if (id == null || id.isBlank()) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "id must not be null or blank");
        }
    }

    private void validateIndexParams(String indexName, String id, Object document) {
        validateIndexNameAndId(indexName, id);
        if (document == null) {
            throw new ClientException(CommonErrorCode.ILLEGAL_ARGUMENT, "document must not be null");
        }
    }

    // ==================== 子类扩展点 ====================

    protected abstract void doIndex(String indexName, String id, Object document);

    protected abstract void doBulkIndex(String indexName, List<Map<String, Object>> documents);

    protected abstract void doDelete(String indexName, String id);

    protected abstract Map<String, Object> doGet(String indexName, String id);

    protected abstract SearchResult doSearch(SearchQuery query);

    protected abstract Map<String, Long> doAggregate(String indexName, String fieldName, SearchQuery query);

    protected abstract boolean doExists(String indexName, String id);

    protected abstract boolean doExistsIndex(String indexName);

    protected abstract boolean doCreateIndex(String indexName);

    protected abstract boolean doDeleteIndex(String indexName);

    protected abstract void doRefresh(String indexName);

    protected abstract long doCount(String indexName);

    protected abstract void doBulkDelete(String indexName, List<String> ids);

    protected abstract void doUpdate(String indexName, String id, Map<String, Object> document);
}
