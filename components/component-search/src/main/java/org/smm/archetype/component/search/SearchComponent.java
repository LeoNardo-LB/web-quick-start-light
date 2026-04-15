package org.smm.archetype.component.search;

import org.smm.archetype.component.dto.SearchQuery;
import org.smm.archetype.component.dto.SearchResult;

import java.util.List;
import java.util.Map;

/**
 * 搜索组件接口。
 * <p>
 * 提供完整的索引和搜索功能，支持单条索引、批量索引、删除、搜索、聚合、
 * 索引管理等操作。
 */
public interface SearchComponent {

    /**
     * 索引单条文档
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     * @param document  文档内容
     */
    void index(String indexName, String id, Object document);

    /**
     * 批量索引文档
     *
     * @param indexName 索引名称
     * @param documents 文档列表（Map 包含 "id" 键作为文档 ID）
     */
    void bulkIndex(String indexName, List<Map<String, Object>> documents);

    /**
     * 删除文档
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     */
    void delete(String indexName, String id);

    /**
     * 获取单条文档
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     * @return 文档内容，不存在返回 null
     */
    Map<String, Object> get(String indexName, String id);

    /**
     * 搜索文档
     *
     * @param query 搜索查询
     * @return 搜索结果
     */
    SearchResult search(SearchQuery query);

    /**
     * 聚合查询
     *
     * @param indexName    索引名称
     * @param fieldName    聚合字段
     * @param query        搜索查询（可为 null）
     * @return 聚合结果（field -> count）
     */
    Map<String, Long> aggregate(String indexName, String fieldName, SearchQuery query);

    /**
     * 判断文档是否存在
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     * @return 存在返回 true
     */
    boolean exists(String indexName, String id);

    /**
     * 判断索引是否存在
     *
     * @param indexName 索引名称
     * @return 存在返回 true
     */
    boolean existsIndex(String indexName);

    /**
     * 创建索引
     *
     * @param indexName 索引名称
     * @return 创建成功返回 true
     */
    boolean createIndex(String indexName);

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @return 删除成功返回 true
     */
    boolean deleteIndex(String indexName);

    /**
     * 刷新索引（使写入可见）
     *
     * @param indexName 索引名称
     */
    void refresh(String indexName);

    /**
     * 获取索引中文档数量
     *
     * @param indexName 索引名称
     * @return 文档数量
     */
    long count(String indexName);

    /**
     * 批量删除文档
     *
     * @param indexName 索引名称
     * @param ids       文档 ID 列表
     */
    void bulkDelete(String indexName, List<String> ids);

    /**
     * 更新文档
     *
     * @param indexName 索引名称
     * @param id        文档 ID
     * @param document  更新内容
     */
    void update(String indexName, String id, Map<String, Object> document);

    /**
     * 搜索文档（返回原始 Map 列表）
     *
     * @param indexName 索引名称
     * @param keyword   关键字
     * @param pageNo    页码
     * @param pageSize  每页大小
     * @return 搜索结果
     */
    SearchResult search(String indexName, String keyword, int pageNo, int pageSize);
}
