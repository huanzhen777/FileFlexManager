package com.huanzhen.fileflexmanager.infrastructure.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@MappedTypes(LocalDateTime.class)
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, parameter.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long time = rs.getLong(columnName);
        return time == 0 ? null : LocalDateTime.ofEpochSecond(time / 1000, 0, ZoneOffset.UTC);
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        long time = rs.getLong(columnIndex);
        return time == 0 ? null : LocalDateTime.ofEpochSecond(time / 1000, 0, ZoneOffset.UTC);
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        long time = cs.getLong(columnIndex);
        return time == 0 ? null : LocalDateTime.ofEpochSecond(time / 1000, 0, ZoneOffset.UTC);
    }
} 