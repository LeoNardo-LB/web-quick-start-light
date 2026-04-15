package org.smm.archetype.shared.util.dal;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * SQLite 兼容的 Instant 类型处理器。
 * <p>
 * SQLite JDBC 驱动对 {@code setTimestamp} 会存储为 epoch 毫秒字符串（如 {@code 1735898400000}），
 * 对 {@code datetime('now')} 默认值生成 {@code yyyy-MM-dd HH:mm:ss} 格式。
 * <p>
 * 此处理器统一使用 ISO-8601 字符串格式写入 Instant，
 * 读取时兼容三种格式：ISO-8601、SQLite datetime、epoch 毫秒。
 */
@MappedTypes(Instant.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class InstantTypeHandler extends BaseTypeHandler<Instant> {

    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.toString());
    }

    @Override
    public Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toInstant(rs.getString(columnName));
    }

    @Override
    public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toInstant(rs.getString(columnIndex));
    }

    @Override
    public Instant getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toInstant(cs.getString(columnIndex));
    }

    /**
     * 将字符串解析为 Instant。支持三种格式：
     * <ul>
     *     <li>ISO-8601 格式（如 {@code 2025-01-15T08:00:00Z}）—— Java 端写入</li>
     *     <li>SQLite datetime 格式（如 {@code 2025-01-15 08:00:00}）—— 数据库默认值</li>
     *     <li>Epoch 毫秒（如 {@code 1735898400000}）—— 旧数据兼容</li>
     * </ul>
     */
    private Instant toInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // 1. 尝试 ISO-8601 格式
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        // 2. 尝试 SQLite datetime 格式
        try {
            return LocalDateTime.parse(value, SQLITE_DATETIME_FORMATTER)
                    .toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }
        // 3. 尝试 epoch 毫秒
        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("Cannot parse Instant from: " + value);
    }
}
