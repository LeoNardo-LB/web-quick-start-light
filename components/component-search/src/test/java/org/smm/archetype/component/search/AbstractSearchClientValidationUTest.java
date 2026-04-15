package org.smm.archetype.component.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
// MockitoExtension removed — this test uses no mocks, only real TestSearchClient subclass
import org.smm.archetype.component.dto.SearchQuery;
import org.smm.archetype.component.dto.SearchResult;
import org.smm.archetype.exception.ClientException;
import org.smm.archetype.exception.CommonErrorCode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AbstractSearchComponent 参数校验与异常包装测试。
 * <p>
 * 通过内部 TestSearchClient 子类验证 Template Method 模式中的：
 * - 参数校验（null / blank 检查）
 * - 异常包装（do* 方法异常 → ClientException）
 */
@DisplayName("AbstractSearchComponent 参数校验与异常包装")
class AbstractSearchComponentValidationUTest {

    private static final String INDEX = "test-index";
    private static final String ID = "test-id";

    /**
     * 测试用具体子类，跟踪 do* 方法调用，可控制是否抛异常。
     */
    private static class TestSearchClient extends AbstractSearchComponent {

        private final AtomicBoolean shouldThrow = new AtomicBoolean(false);

        void setShouldThrow(boolean value) {
            this.shouldThrow.set(value);
        }

        @Override
        protected void doIndex(String indexName, String id, Object document) {
            throwIfRequested();
        }

        @Override
        protected void doBulkIndex(String indexName, List<Map<String, Object>> documents) {
            throwIfRequested();
        }

        @Override
        protected void doDelete(String indexName, String id) {
            throwIfRequested();
        }

        @Override
        protected Map<String, Object> doGet(String indexName, String id) {
            throwIfRequested();
            return new HashMap<>();
        }

        @Override
        protected SearchResult doSearch(SearchQuery query) {
            throwIfRequested();
            return new SearchResult(0L, List.of(), 1, 10);
        }

        @Override
        protected Map<String, Long> doAggregate(String indexName, String fieldName, SearchQuery query) {
            throwIfRequested();
            return new HashMap<>();
        }

        @Override
        protected boolean doExists(String indexName, String id) {
            return false;
        }

        @Override
        protected boolean doExistsIndex(String indexName) {
            return false;
        }

        @Override
        protected boolean doCreateIndex(String indexName) {
            return true;
        }

        @Override
        protected boolean doDeleteIndex(String indexName) {
            return true;
        }

        @Override
        protected void doRefresh(String indexName) {
            // no-op
        }

        @Override
        protected long doCount(String indexName) {
            return 0L;
        }

        @Override
        protected void doBulkDelete(String indexName, List<String> ids) {
            throwIfRequested();
        }

        @Override
        protected void doUpdate(String indexName, String id, Map<String, Object> document) {
            throwIfRequested();
        }

        private void throwIfRequested() {
            if (shouldThrow.get()) {
                throw new RuntimeException("模拟 do* 方法异常");
            }
        }
    }

    private final TestSearchClient client = new TestSearchClient();

    // ==================== index 参数校验 ====================

    @Nested
    @DisplayName("index - 参数校验")
    class IndexValidation {

        @Test
        @DisplayName("null indexName 应抛出 ClientException")
        void should_reject_null_indexName() {
            Map<String, Object> doc = new HashMap<>();
            assertThatThrownBy(() -> client.index(null, ID, doc))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("blank indexName 应抛出 ClientException")
        void should_reject_blank_indexName() {
            Map<String, Object> doc = new HashMap<>();
            assertThatThrownBy(() -> client.index("  ", ID, doc))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null id 应抛出 ClientException")
        void should_reject_null_id() {
            Map<String, Object> doc = new HashMap<>();
            assertThatThrownBy(() -> client.index(INDEX, null, doc))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null document 应抛出 ClientException")
        void should_reject_null_document() {
            assertThatThrownBy(() -> client.index(INDEX, ID, null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doIndex_exception() {
            client.setShouldThrow(true);
            Map<String, Object> doc = new HashMap<>();
            assertThatThrownBy(() -> client.index(INDEX, ID, doc))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== bulkIndex 参数校验 ====================

    @Nested
    @DisplayName("bulkIndex - 参数校验")
    class BulkIndexValidation {

        @Test
        @DisplayName("null documents 应抛出 ClientException")
        void should_reject_null_documents() {
            assertThatThrownBy(() -> client.bulkIndex(INDEX, null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("空 documents 应抛出 ClientException")
        void should_reject_empty_documents() {
            assertThatThrownBy(() -> client.bulkIndex(INDEX, Collections.emptyList()))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doBulkIndex_exception() {
            client.setShouldThrow(true);
            Map<String, Object> doc = new HashMap<>();
            doc.put("id", "1");
            assertThatThrownBy(() -> client.bulkIndex(INDEX, List.of(doc)))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== delete 参数校验 ====================

    @Nested
    @DisplayName("delete - 参数校验")
    class DeleteValidation {

        @Test
        @DisplayName("null indexName 应抛出 ClientException")
        void should_reject_null_indexName() {
            assertThatThrownBy(() -> client.delete(null, ID))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null id 应抛出 ClientException")
        void should_reject_null_id() {
            assertThatThrownBy(() -> client.delete(INDEX, null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doDelete_exception() {
            client.setShouldThrow(true);
            assertThatThrownBy(() -> client.delete(INDEX, ID))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== get 参数校验 ====================

    @Nested
    @DisplayName("get - 参数校验")
    class GetValidation {

        @Test
        @DisplayName("null indexName 应抛出 ClientException")
        void should_reject_null_indexName() {
            assertThatThrownBy(() -> client.get(null, ID))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("null id 应抛出 ClientException")
        void should_reject_null_id() {
            assertThatThrownBy(() -> client.get(INDEX, null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doGet_exception() {
            client.setShouldThrow(true);
            assertThatThrownBy(() -> client.get(INDEX, ID))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== search(SearchQuery) 参数校验 ====================

    @Nested
    @DisplayName("search(SearchQuery) - 参数校验")
    class SearchQueryValidation {

        @Test
        @DisplayName("null query 应抛出 ClientException")
        void should_reject_null_query() {
            assertThatThrownBy(() -> client.search((SearchQuery) null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("query 中 null indexName 应抛出 ClientException")
        void should_reject_null_indexName_in_query() {
            SearchQuery query = new SearchQuery("test", null, 1, 10);
            assertThatThrownBy(() -> client.search(query))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doSearch_exception() {
            client.setShouldThrow(true);
            SearchQuery query = new SearchQuery("test", INDEX, 1, 10);
            assertThatThrownBy(() -> client.search(query))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== aggregate 参数校验 ====================

    @Nested
    @DisplayName("aggregate - 参数校验")
    class AggregateValidation {

        @Test
        @DisplayName("null fieldName 应抛出 ClientException")
        void should_reject_null_fieldName() {
            SearchQuery query = new SearchQuery(null, INDEX, 1, 10);
            assertThatThrownBy(() -> client.aggregate(INDEX, null, query))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("blank fieldName 应抛出 ClientException")
        void should_reject_blank_fieldName() {
            SearchQuery query = new SearchQuery(null, INDEX, 1, 10);
            assertThatThrownBy(() -> client.aggregate(INDEX, "  ", query))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doAggregate_exception() {
            client.setShouldThrow(true);
            SearchQuery query = new SearchQuery(null, INDEX, 1, 10);
            assertThatThrownBy(() -> client.aggregate(INDEX, "field", query))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== exists 参数校验 ====================

    @Nested
    @DisplayName("exists - 参数校验")
    class ExistsValidation {

        @Test
        @DisplayName("null indexName 应抛出 ClientException")
        void should_reject_null_indexName() {
            assertThatThrownBy(() -> client.exists(null, ID))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    // ==================== bulkDelete 参数校验 ====================

    @Nested
    @DisplayName("bulkDelete - 参数校验")
    class BulkDeleteValidation {

        @Test
        @DisplayName("null ids 应抛出 ClientException")
        void should_reject_null_ids() {
            assertThatThrownBy(() -> client.bulkDelete(INDEX, null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("空 ids 应抛出 ClientException")
        void should_reject_empty_ids() {
            assertThatThrownBy(() -> client.bulkDelete(INDEX, Collections.emptyList()))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doBulkDelete_exception() {
            client.setShouldThrow(true);
            assertThatThrownBy(() -> client.bulkDelete(INDEX, List.of("id1")))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== update 参数校验 ====================

    @Nested
    @DisplayName("update - 参数校验")
    class UpdateValidation {

        @Test
        @DisplayName("null document 应抛出 ClientException")
        void should_reject_null_document() {
            assertThatThrownBy(() -> client.update(INDEX, ID, null))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("空 document 应抛出 ClientException")
        void should_reject_empty_document() {
            assertThatThrownBy(() -> client.update(INDEX, ID, Collections.emptyMap()))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("do* 方法异常应包装为 SEARCH_OPERATION_FAILED")
        void should_wrap_doUpdate_exception() {
            client.setShouldThrow(true);
            Map<String, Object> doc = new HashMap<>();
            doc.put("key", "value");
            assertThatThrownBy(() -> client.update(INDEX, ID, doc))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== search(String, String, int, int) 参数校验 ====================

    @Nested
    @DisplayName("search(String, String, int, int) - 参数校验")
    class SearchConvenienceValidation {

        @Test
        @DisplayName("null indexName 应抛出 ClientException")
        void should_reject_null_indexName() {
            assertThatThrownBy(() -> client.search(null, "keyword", 1, 10))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("blank indexName 应抛出 ClientException")
        void should_reject_blank_indexName() {
            assertThatThrownBy(() -> client.search("  ", "keyword", 1, 10))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.ILLEGAL_ARGUMENT);
        }

        @Test
        @DisplayName("便捷方法应正确构造 SearchQuery 并委托")
        void should_delegate_to_search_query_method() {
            // 正常调用不应抛异常
            client.search(INDEX, "test", 1, 10);
            // 验证 setShouldThrow 时便捷方法也会包装异常
            client.setShouldThrow(true);
            assertThatThrownBy(() -> client.search(INDEX, "test", 1, 10))
                    .isInstanceOf(ClientException.class)
                    .extracting(e -> ((ClientException) e).getErrorCode())
                    .isEqualTo(CommonErrorCode.SEARCH_OPERATION_FAILED);
            client.setShouldThrow(false);
        }
    }

    // ==================== 异常包装通用验证 ====================

    @Nested
    @DisplayName("异常包装 - RuntimeException → ClientException")
    class ExceptionWrapping {

        @Test
        @DisplayName("包装后的异常应保留原始异常为 cause")
        void should_preserve_cause() {
            client.setShouldThrow(true);
            Map<String, Object> doc = new HashMap<>();
            assertThatThrownBy(() -> client.index(INDEX, ID, doc))
                    .isInstanceOf(ClientException.class)
                    .satisfies(ex -> {
                        ClientException ce = (ClientException) ex;
                        assertThat(ce.getCause()).isNotNull();
                        assertThat(ce.getCause()).isInstanceOf(RuntimeException.class);
                        assertThat(ce.getCause().getMessage()).isEqualTo("模拟 do* 方法异常");
                    });
            client.setShouldThrow(false);
        }

        @Test
        @DisplayName("包装后的异常消息应包含操作描述")
        void should_include_operation_description() {
            client.setShouldThrow(true);
            Map<String, Object> doc = new HashMap<>();
            assertThatThrownBy(() -> client.index(INDEX, ID, doc))
                    .isInstanceOf(ClientException.class)
                    .satisfies(ex -> {
                        assertThat(ex.getMessage()).contains("索引文档失败");
                        assertThat(ex.getMessage()).contains(INDEX);
                    });
            client.setShouldThrow(false);
        }
    }
}
