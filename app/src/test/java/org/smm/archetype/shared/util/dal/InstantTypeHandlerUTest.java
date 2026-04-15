package org.smm.archetype.shared.util.dal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InstantTypeHandler 单元测试 — 验证 SQLite 兼容的 Instant ↔ TEXT 转换。
 * <p>
 * 此 TypeHandler 是本次变更新增代码，支持三种格式：
 * <ul>
 *     <li>ISO-8601（Java 端写入）</li>
 *     <li>SQLite datetime（数据库默认值）</li>
 *     <li>Epoch 毫秒（旧数据兼容）</li>
 * </ul>
 */
@DisplayName("InstantTypeHandler")
class InstantTypeHandlerUTest extends UnitTestBase {

    private final InstantTypeHandler handler = new InstantTypeHandler();

    // =========================================================================
    // 读取路径：三种格式的 toInstant 转换
    // =========================================================================

    @Nested
    @DisplayName("getNullableResult — ISO-8601 格式")
    class Iso8601Format {

        @Test
        @DisplayName("MFT: 标准 ISO-8601 字符串应正确解析为 Instant")
        void should_parse_iso8601_string() throws Exception {
            // given
            String isoStr = "2025-01-15T08:30:45.123456789Z";
            ResultSet rs = mockResultSet("create_time", isoStr);

            // when
            Instant result = handler.getNullableResult(rs, "create_time");

            // then
            assertThat(result).isEqualTo(Instant.parse(isoStr));
        }

        @Test
        @DisplayName("MFT: ISO-8601 带 UTC 偏移应正确解析")
        void should_parse_iso8601_with_utc_offset() throws Exception {
            // given
            String isoStr = "2025-06-15T10:00:00Z";
            ResultSet rs = mockResultSet("create_time", isoStr);

            // when
            Instant result = handler.getNullableResult(rs, "create_time");

            // then
            assertThat(result).isEqualTo(Instant.parse(isoStr));
        }
    }

    @Nested
    @DisplayName("getNullableResult — SQLite datetime 格式")
    class SqliteDatetimeFormat {

        @Test
        @DisplayName("MFT: SQLite datetime('now') 格式应正确解析")
        void should_parse_sqlite_datetime() throws Exception {
            // given — SQLite datetime('now') 生成格式: yyyy-MM-dd HH:mm:ss
            String sqliteStr = "2025-01-15 08:30:45";
            ResultSet rs = mockResultSet("create_time", sqliteStr);
            Instant expected = LocalDateTime.parse(sqliteStr,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .toInstant(ZoneOffset.UTC);

            // when
            Instant result = handler.getNullableResult(rs, "create_time");

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("getNullableResult — Epoch 毫秒格式")
    class EpochMillisFormat {

        @Test
        @DisplayName("MFT: Epoch 毫秒字符串应正确解析")
        void should_parse_epoch_millis() throws Exception {
            // given — SQLite JDBC setTimestamp 存储格式
            Instant expected = Instant.ofEpochMilli(1735898400000L);
            ResultSet rs = mockResultSet("create_time", "1735898400000");

            // when
            Instant result = handler.getNullableResult(rs, "create_time");

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("getNullableResult — 边界值与异常")
    class BoundaryAndError {

        @Test
        @DisplayName("INV: null 输入应返回 null")
        void should_return_null_for_null_input() throws Exception {
            ResultSet rs = mockResultSet("create_time", null);

            Instant result = handler.getNullableResult(rs, "create_time");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("INV: 空白字符串应返回 null")
        void should_return_null_for_blank_input() throws Exception {
            ResultSet rs = mockResultSet("create_time", "   ");

            Instant result = handler.getNullableResult(rs, "create_time");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("INV: 空字符串应返回 null")
        void should_return_null_for_empty_input() throws Exception {
            ResultSet rs = mockResultSet("create_time", "");

            Instant result = handler.getNullableResult(rs, "create_time");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("DIR: 非法格式应抛出 IllegalArgumentException")
        void should_throw_for_invalid_format() throws Exception {
            ResultSet rs = mockResultSet("create_time", "not-a-date-at-all");

            assertThatThrownBy(() -> handler.getNullableResult(rs, "create_time"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot parse Instant from: not-a-date-at-all");
        }
    }

    // =========================================================================
    // 写入路径
    // =========================================================================

    @Nested
    @DisplayName("setNonNullParameter — 写入路径")
    class WritePath {

        @Test
        @DisplayName("MFT: Instant 应以 ISO-8601 字符串写入 PreparedStatement")
        void should_write_as_iso8601_string() throws Exception {
            // given
            Instant instant = Instant.parse("2025-06-15T10:00:00Z");
            PreparedStatement ps = mock(PreparedStatement.class);

            // when
            handler.setNonNullParameter(ps, 1, instant, null);

            // then
            verify(ps).setString(1, "2025-06-15T10:00:00Z");
        }
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private static ResultSet mockResultSet(String columnName, String value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(columnName)).thenReturn(value);
        return rs;
    }
}
