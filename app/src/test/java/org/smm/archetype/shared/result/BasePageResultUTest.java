package org.smm.archetype.shared.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.shared.pagination.PageResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BasePageResult 分页响应包装")
class BasePageResultUTest {

    @Test
    @DisplayName("from(PageResult) 构建正确的分页响应")
    void shouldBuildFromPageResult() {
        var pageResult = PageResult.of(
                List.of("a", "b", "c"), 100, 2, 20);
        var result = BasePageResult.from(pageResult);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData().size());
        assertEquals(100, result.getTotal());
        assertEquals(2, result.getPageNo());
        assertEquals(20, result.getPageSize());
    }

    @Test
    @DisplayName("from(PageResult) 空结果构建")
    void shouldBuildEmptyFromPageResult() {
        var pageResult = PageResult.of(List.of(), 0, 1, 10);
        var result = BasePageResult.from(pageResult);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getData().size());
        assertEquals(0, result.getTotal());
    }
}
