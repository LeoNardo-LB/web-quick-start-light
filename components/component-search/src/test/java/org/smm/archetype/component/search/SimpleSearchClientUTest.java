package org.smm.archetype.component.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.component.dto.SearchQuery;
import org.smm.archetype.component.dto.SearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleSearchComponent 单元测试。
 * <p>
 * 测试所有 15 个公开方法，基于 ConcurrentHashMap 内存存储。
 */
@DisplayName("SimpleSearchComponent 功能测试")
class SimpleSearchComponentUTest {

    private SimpleSearchComponent client;
    private static final String INDEX = "test-index";

    @BeforeEach
    void setUp() {
        client = new SimpleSearchComponent();
    }

    // ==================== index ====================

    @Nested
    @DisplayName("index - 索引单条文档")
    class IndexTests {

        @Test
        @DisplayName("Map 文档应正确存储，并包含 _id 字段")
        void should_index_map_document() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "Alice");
            doc.put("age", 30);

            client.index(INDEX, "doc1", doc);

            Map<String, Object> result = client.get(INDEX, "doc1");
            assertThat(result).isNotNull();
            assertThat(result).containsEntry("name", "Alice")
                    .containsEntry("age", 30);
            // get 返回的文档不应包含 _id
            assertThat(result).doesNotContainKey("_id");
        }

        @Test
        @DisplayName("POJO 文档应通过 fastjson2 序列化后存储")
        void should_index_pojo_document() {
            Person person = new Person("Bob", 25, "Engineering");

            client.index(INDEX, "doc2", person);

            Map<String, Object> result = client.get(INDEX, "doc2");
            assertThat(result).isNotNull();
            assertThat(result).containsEntry("name", "Bob")
                    .containsEntry("age", 25)
                    .containsEntry("department", "Engineering");
        }

        @Test
        @DisplayName("索引后 exists 应返回 true")
        void should_exist_after_index() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "Charlie");

            client.index(INDEX, "doc3", doc);

            assertThat(client.exists(INDEX, "doc3")).isTrue();
        }
    }

    // ==================== bulkIndex ====================

    @Nested
    @DisplayName("bulkIndex - 批量索引")
    class BulkIndexTests {

        @Test
        @DisplayName("包含 'id' 键的文档应使用 id 作为文档 ID")
        void should_use_id_key_as_document_id() {
            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("id", "bulk1");
            doc1.put("name", "Alice");

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("id", "bulk2");
            doc2.put("name", "Bob");

            client.bulkIndex(INDEX, List.of(doc1, doc2));

            assertThat(client.exists(INDEX, "bulk1")).isTrue();
            assertThat(client.exists(INDEX, "bulk2")).isTrue();
            assertThat(client.get(INDEX, "bulk1")).containsEntry("name", "Alice");
        }

        @Test
        @DisplayName("包含 '_id' 键的文档应使用 _id 作为文档 ID")
        void should_use_underscore_id_key_as_document_id() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("_id", "bid1");
            doc.put("name", "Charlie");

            client.bulkIndex(INDEX, List.of(doc));

            assertThat(client.exists(INDEX, "bid1")).isTrue();
            assertThat(client.get(INDEX, "bid1")).containsEntry("name", "Charlie");
        }

        @Test
        @DisplayName("无 id 和 _id 的文档应自动生成 UUID")
        void should_generate_uuid_when_no_id() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "NoId");

            client.bulkIndex(INDEX, List.of(doc));

            // 文档应被存储，索引中应有 1 条
            assertThat(client.count(INDEX)).isEqualTo(1L);
        }

        @Test
        @DisplayName("批量索引后所有文档应可被搜索到")
        void should_search_all_bulk_indexed_docs() {
            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("id", "b1");
            doc1.put("title", "Java Guide");

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("id", "b2");
            doc2.put("title", "Python Guide");

            client.bulkIndex(INDEX, List.of(doc1, doc2));

            SearchQuery query = new SearchQuery(null, INDEX, 1, 10);
            SearchResult result = client.search(query);
            assertThat(result.total()).isEqualTo(2L);
            assertThat(result.records()).hasSize(2);
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete - 删除文档")
    class DeleteTests {

        @Test
        @DisplayName("删除已存在的文档应成功")
        void should_delete_existing_document() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "ToDelete");

            client.index(INDEX, "del1", doc);
            assertThat(client.exists(INDEX, "del1")).isTrue();

            client.delete(INDEX, "del1");

            assertThat(client.exists(INDEX, "del1")).isFalse();
            assertThat(client.get(INDEX, "del1")).isNull();
        }

        @Test
        @DisplayName("删除不存在的文档不应抛异常")
        void should_not_throw_when_deleting_non_existing() {
            // 不应抛出异常
            client.delete(INDEX, "non-existing");
            assertThat(client.exists(INDEX, "non-existing")).isFalse();
        }
    }

    // ==================== get ====================

    @Nested
    @DisplayName("get - 获取文档")
    class GetTests {

        @Test
        @DisplayName("获取已存在的文档，返回结果不应包含 _id")
        void should_get_existing_document_without_id_field() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "Getter");
            doc.put("score", 95);

            client.index(INDEX, "get1", doc);

            Map<String, Object> result = client.get(INDEX, "get1");
            assertThat(result).isNotNull();
            assertThat(result).containsEntry("name", "Getter")
                    .containsEntry("score", 95);
            assertThat(result).doesNotContainKey("_id");
        }

        @Test
        @DisplayName("获取不存在的文档应返回 null")
        void should_return_null_for_non_existing() {
            Map<String, Object> result = client.get(INDEX, "ghost");
            assertThat(result).isNull();
        }
    }

    // ==================== search(SearchQuery) ====================

    @Nested
    @DisplayName("search(SearchQuery) - 搜索文档")
    class SearchQueryTests {

        @BeforeEach
        void indexTestData() {
            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("title", "Java Programming Guide");
            doc1.put("author", "Alice");
            client.index(INDEX, "s1", doc1);

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("title", "Python Programming Guide");
            doc2.put("author", "Bob");
            client.index(INDEX, "s2", doc2);

            Map<String, Object> doc3 = new HashMap<>();
            doc3.put("title", "Java Advanced Topics");
            doc3.put("author", "Charlie");
            client.index(INDEX, "s3", doc3);
        }

        @Test
        @DisplayName("关键字匹配应返回包含关键字的文档")
        void should_match_keyword() {
            SearchQuery query = new SearchQuery("Java", INDEX, 1, 10);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(2L);
            assertThat(result.records()).hasSize(2);
            assertThat(result.records())
                    .allSatisfy(record ->
                            assertThat(record.values().toString()).contains("Java"));
        }

        @Test
        @DisplayName("null 关键字应匹配所有文档")
        void should_match_all_with_null_keyword() {
            SearchQuery query = new SearchQuery(null, INDEX, 1, 10);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(3L);
            assertThat(result.records()).hasSize(3);
        }

        @Test
        @DisplayName("空关键字应匹配所有文档")
        void should_match_all_with_blank_keyword() {
            SearchQuery query = new SearchQuery("  ", INDEX, 1, 10);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(3L);
        }

        @Test
        @DisplayName("分页应正确返回指定页的数据")
        void should_support_pagination() {
            SearchQuery query = new SearchQuery(null, INDEX, 1, 2);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(3L);
            assertThat(result.records()).hasSize(2);
            assertThat(result.pageNo()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("第二页应返回剩余数据")
        void should_return_second_page() {
            SearchQuery query = new SearchQuery(null, INDEX, 2, 2);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(3L);
            assertThat(result.records()).hasSize(1);
            assertThat(result.pageNo()).isEqualTo(2);
        }

        @Test
        @DisplayName("页码超出范围应返回空列表")
        void should_return_empty_when_page_beyond_results() {
            SearchQuery query = new SearchQuery(null, INDEX, 100, 10);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(3L);
            assertThat(result.records()).isEmpty();
        }

        @Test
        @DisplayName("无匹配文档应返回空结果")
        void should_return_empty_for_no_match() {
            SearchQuery query = new SearchQuery("NonExistentKeyword", INDEX, 1, 10);
            SearchResult result = client.search(query);

            assertThat(result.total()).isEqualTo(0L);
            assertThat(result.records()).isEmpty();
        }
    }

    // ==================== aggregate ====================

    @Nested
    @DisplayName("aggregate - 聚合查询")
    class AggregateTests {

        @BeforeEach
        void indexTestData() {
            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("category", "tech");
            doc1.put("author", "Alice");
            client.index(INDEX, "agg1", doc1);

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("category", "tech");
            doc2.put("author", "Bob");
            client.index(INDEX, "agg2", doc2);

            Map<String, Object> doc3 = new HashMap<>();
            doc3.put("category", "science");
            doc3.put("author", "Charlie");
            client.index(INDEX, "agg3", doc3);

            Map<String, Object> doc4 = new HashMap<>();
            doc4.put("author", "Dave");
            // category 为 null
            client.index(INDEX, "agg4", doc4);
        }

        @Test
        @DisplayName("按字段聚合应返回正确的计数")
        void should_aggregate_by_field() {
            Map<String, Long> result = client.aggregate(INDEX, "category", null);

            assertThat(result).containsEntry("tech", 2L)
                    .containsEntry("science", 1L)
                    .containsEntry("null", 1L);
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("null query 应聚合所有文档")
        void should_aggregate_all_with_null_query() {
            Map<String, Long> result = client.aggregate(INDEX, "author", null);

            assertThat(result).hasSize(4);
            assertThat(result).containsEntry("Alice", 1L)
                    .containsEntry("Bob", 1L)
                    .containsEntry("Charlie", 1L)
                    .containsEntry("Dave", 1L);
        }

        @Test
        @DisplayName("null 字段值应统计为 'null' 键")
        void should_count_null_field_values_as_null_key() {
            Map<String, Long> result = client.aggregate(INDEX, "category", null);

            assertThat(result).containsKey("null");
            assertThat(result.get("null")).isEqualTo(1L);
        }

        @Test
        @DisplayName("带关键字的聚合应只聚合匹配文档")
        void should_aggregate_with_keyword_filter() {
            SearchQuery query = new SearchQuery("Alice", INDEX, 1, 10);
            Map<String, Long> result = client.aggregate(INDEX, "category", query);

            assertThat(result).hasSize(1);
            assertThat(result).containsEntry("tech", 1L);
        }
    }

    // ==================== exists ====================

    @Nested
    @DisplayName("exists - 判断文档是否存在")
    class ExistsTests {

        @Test
        @DisplayName("已存在的文档应返回 true")
        void should_return_true_for_existing() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "Exist");
            client.index(INDEX, "ex1", doc);

            assertThat(client.exists(INDEX, "ex1")).isTrue();
        }

        @Test
        @DisplayName("不存在的文档应返回 false")
        void should_return_false_for_non_existing() {
            assertThat(client.exists(INDEX, "ghost")).isFalse();
        }
    }

    // ==================== existsIndex ====================

    @Nested
    @DisplayName("existsIndex - 判断索引是否存在")
    class ExistsIndexTests {

        @Test
        @DisplayName("createIndex 后 existsIndex 应返回 true")
        void should_exist_after_createIndex() {
            assertThat(client.existsIndex(INDEX)).isFalse();

            client.createIndex(INDEX);

            assertThat(client.existsIndex(INDEX)).isTrue();
        }

        @Test
        @DisplayName("index 文档后索引也应存在")
        void should_exist_after_index() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "AutoIndex");
            client.index(INDEX, "idx1", doc);

            assertThat(client.existsIndex(INDEX)).isTrue();
        }

        @Test
        @DisplayName("deleteIndex 后 existsIndex 应返回 false")
        void should_not_exist_after_deleteIndex() {
            client.createIndex(INDEX);
            assertThat(client.existsIndex(INDEX)).isTrue();

            client.deleteIndex(INDEX);

            assertThat(client.existsIndex(INDEX)).isFalse();
        }
    }

    // ==================== createIndex ====================

    @Nested
    @DisplayName("createIndex - 创建索引")
    class CreateIndexTests {

        @Test
        @DisplayName("创建索引应始终返回 true")
        void should_always_return_true() {
            boolean result = client.createIndex(INDEX);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("重复创建索引不应抛异常")
        void should_not_throw_on_duplicate_create() {
            client.createIndex(INDEX);
            boolean result = client.createIndex(INDEX);
            assertThat(result).isTrue();
        }
    }

    // ==================== deleteIndex ====================

    @Nested
    @DisplayName("deleteIndex - 删除索引")
    class DeleteIndexTests {

        @Test
        @DisplayName("删除索引应移除所有相关文档")
        void should_remove_all_documents() {
            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("name", "A");
            client.index(INDEX, "d1", doc1);

            Map<String, Object> doc2 = new HashMap<>();
            doc2.put("name", "B");
            client.index(INDEX, "d2", doc2);

            assertThat(client.count(INDEX)).isEqualTo(2L);

            client.deleteIndex(INDEX);

            assertThat(client.count(INDEX)).isEqualTo(0L);
            assertThat(client.exists(INDEX, "d1")).isFalse();
            assertThat(client.exists(INDEX, "d2")).isFalse();
        }

        @Test
        @DisplayName("删除索引应移除索引标记")
        void should_remove_index_marker() {
            client.createIndex(INDEX);
            client.deleteIndex(INDEX);

            assertThat(client.existsIndex(INDEX)).isFalse();
        }

        @Test
        @DisplayName("删除索引应不影响其他索引的文档")
        void should_not_affect_other_indices() {
            String otherIndex = "other-index";
            Map<String, Object> doc1 = new HashMap<>();
            doc1.put("name", "Keep");
            client.index(INDEX, "k1", doc1);
            client.index(otherIndex, "k2", doc1);

            client.deleteIndex(INDEX);

            assertThat(client.count(INDEX)).isEqualTo(0L);
            assertThat(client.count(otherIndex)).isEqualTo(1L);
        }
    }

    // ==================== refresh ====================

    @Nested
    @DisplayName("refresh - 刷新索引")
    class RefreshTests {

        @Test
        @DisplayName("刷新不应抛出异常")
        void should_not_throw() {
            client.createIndex(INDEX);
            // 内存实现无需刷新，调用后不应抛异常
            client.refresh(INDEX);
        }
    }

    // ==================== count ====================

    @Nested
    @DisplayName("count - 文档计数")
    class CountTests {

        @Test
        @DisplayName("索引多条文档后 count 应返回正确数量")
        void should_count_indexed_documents() {
            for (int i = 0; i < 5; i++) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("seq", i);
                client.index(INDEX, "cnt" + i, doc);
            }

            assertThat(client.count(INDEX)).isEqualTo(5L);
        }

        @Test
        @DisplayName("空索引 count 应返回 0")
        void should_return_zero_for_empty_index() {
            assertThat(client.count(INDEX)).isEqualTo(0L);
        }

        @Test
        @DisplayName("删除文档后 count 应减少")
        void should_decrease_after_delete() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "Count");
            client.index(INDEX, "cd1", doc);
            client.index(INDEX, "cd2", doc);

            assertThat(client.count(INDEX)).isEqualTo(2L);

            client.delete(INDEX, "cd1");

            assertThat(client.count(INDEX)).isEqualTo(1L);
        }
    }

    // ==================== bulkDelete ====================

    @Nested
    @DisplayName("bulkDelete - 批量删除")
    class BulkDeleteTests {

        @Test
        @DisplayName("批量删除多个文档应全部移除")
        void should_delete_multiple_documents() {
            for (int i = 0; i < 5; i++) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("seq", i);
                client.index(INDEX, "bd" + i, doc);
            }

            client.bulkDelete(INDEX, List.of("bd0", "bd2", "bd4"));

            assertThat(client.count(INDEX)).isEqualTo(2L);
            assertThat(client.exists(INDEX, "bd0")).isFalse();
            assertThat(client.exists(INDEX, "bd1")).isTrue();
            assertThat(client.exists(INDEX, "bd2")).isFalse();
            assertThat(client.exists(INDEX, "bd3")).isTrue();
            assertThat(client.exists(INDEX, "bd4")).isFalse();
        }
    }

    // ==================== update ====================

    @Nested
    @DisplayName("update - 更新文档")
    class UpdateTests {

        @Test
        @DisplayName("更新已存在文档应合并字段")
        void should_merge_fields_for_existing_document() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("name", "Original");
            doc.put("version", 1);
            client.index(INDEX, "upd1", doc);

            Map<String, Object> update = new HashMap<>();
            update.put("version", 2);
            update.put("status", "updated");
            client.update(INDEX, "upd1", update);

            Map<String, Object> result = client.get(INDEX, "upd1");
            assertThat(result).containsEntry("name", "Original")
                    .containsEntry("version", 2)
                    .containsEntry("status", "updated");
            // 更新后 _id 不应出现在返回结果中
            assertThat(result).doesNotContainKey("_id");
        }

        @Test
        @DisplayName("更新不存在的文档应为空操作")
        void should_be_noop_for_non_existing_document() {
            Map<String, Object> update = new HashMap<>();
            update.put("name", "Ghost");

            client.update(INDEX, "non-existing", update);

            assertThat(client.exists(INDEX, "non-existing")).isFalse();
            assertThat(client.count(INDEX)).isEqualTo(0L);
        }
    }

    // ==================== search(String, String, int, int) ====================

    @Nested
    @DisplayName("search(String, String, int, int) - 便捷搜索")
    class SearchConvenienceTests {

        @Test
        @DisplayName("便捷方法应正确委托给 search(SearchQuery)")
        void should_delegate_to_search_query() {
            Map<String, Object> doc = new HashMap<>();
            doc.put("title", "Java Guide");
            client.index(INDEX, "conv1", doc);

            SearchResult result = client.search(INDEX, "Java", 1, 10);

            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.records()).hasSize(1);
            assertThat(result.pageNo()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("便捷方法分页参数应正确传递")
        void should_pass_pagination_params() {
            for (int i = 0; i < 5; i++) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("item", "item-" + i);
                client.index(INDEX, "p" + i, doc);
            }

            SearchResult result = client.search(INDEX, null, 2, 2);

            assertThat(result.total()).isEqualTo(5L);
            assertThat(result.records()).hasSize(2);
            assertThat(result.pageNo()).isEqualTo(2);
            assertThat(result.pageSize()).isEqualTo(2);
        }
    }

    // ==================== 辅助类 ====================

    /**
     * 测试用 POJO，验证非 Map 对象的序列化索引。
     */
    record Person(String name, int age, String department) {}
}
