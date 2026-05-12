package org.smm.archetype.shared.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PageQuery 分页请求值对象")
class PageQueryUTest {

    @Test
    @DisplayName("正常值创建成功")
    void shouldCreateWithValidValues() {
        var page = new PageQuery(3, 20);
        assertEquals(3, page.pageNo());
        assertEquals(20, page.pageSize());
    }

    @Test
    @DisplayName("pageNo=0 自动修正为 1")
    void shouldFixZeroPageNo() {
        var page = new PageQuery(0, 20);
        assertEquals(1, page.pageNo());
    }

    @Test
    @DisplayName("pageNo 负数自动修正为 1")
    void shouldFixNegativePageNo() {
        var page = new PageQuery(-5, 20);
        assertEquals(1, page.pageNo());
    }

    @Test
    @DisplayName("pageSize=0 自动修正为 10")
    void shouldFixZeroPageSize() {
        var page = new PageQuery(1, 0);
        assertEquals(10, page.pageSize());
    }

    @Test
    @DisplayName("pageSize 负数自动修正为 10")
    void shouldFixNegativePageSize() {
        var page = new PageQuery(1, -5);
        assertEquals(10, page.pageSize());
    }

    @Test
    @DisplayName("pageSize 超限自动修正为 100")
    void shouldCappedPageSize() {
        var page = new PageQuery(1, 999);
        assertEquals(100, page.pageSize());
    }
}
