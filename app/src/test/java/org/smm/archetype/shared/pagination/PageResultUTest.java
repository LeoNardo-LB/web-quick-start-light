package org.smm.archetype.shared.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageResult 分页结果值对象")
class PageResultUTest {

    @Test
    @DisplayName("正常分页结果 totalPages 计算正确")
    void shouldCalculateTotalPages() {
        var list = List.of("a", "b", "c", "d", "e");
        var result = PageResult.of(list, 100, 1, 20);
        assertEquals(5, result.totalPages());
        assertEquals(100, result.total());
        assertEquals(5, result.list().size());
    }

    @Test
    @DisplayName("总数 = 0 时 totalPages = 0")
    void shouldHandleZeroTotal() {
        var result = PageResult.of(List.of(), 0, 1, 20);
        assertEquals(0, result.totalPages());
        assertEquals(0, result.total());
        assertTrue(result.list().isEmpty());
    }

    @Test
    @DisplayName("总数不能整除时 totalPages 向上取整")
    void shouldCeilTotalPages() {
        var result = PageResult.of(List.of("a", "b", "c"), 105, 1, 20);
        assertEquals(6, result.totalPages());
    }

    @Test
    @DisplayName("空结果快捷方法创建零 total")
    void shouldCreateEmptyResult() {
        var result = PageResult.<String>empty(1, 20);
        assertEquals(0, result.total());
        assertEquals(0, result.totalPages());
        assertTrue(result.list().isEmpty());
    }

    @Test
    @DisplayName("pageSize=0 抛出 IllegalArgumentException")
    void shouldRejectZeroPageSize() {
        assertThrows(IllegalArgumentException.class, () -> PageResult.of(List.of(), 0, 1, 0));
    }
}
