package com.backstage.system.service.impl;


import com.backstage.system.domain.database.DatabaseObject;
import com.backstage.system.domain.database.QueryResult;
import com.backstage.system.service.IDatabaseService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatabaseServiceImpl implements IDatabaseService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, List<DatabaseObject>> getDatabaseObjects() {
        Map<String, List<DatabaseObject>> result = new HashMap<>();

        // 获取表
        List<DatabaseObject> tables = jdbcTemplate.query("SHOW TABLES", (rs, rowNum) -> {
            DatabaseObject obj = new DatabaseObject();
            obj.setName(rs.getString(1));
            obj.setType("table");
            return obj;
        });
        result.put("tables", tables);

        // 获取视图、函数、存储过程等类似实现

        return result;
    }

    public List<Map<String, Object>> getTableData(String tableName, int pageIndex, int pageSize) {
        // 1. 参数校验
        if (pageIndex < 1 || pageSize < 1) {
            throw new IllegalArgumentException("分页参数不合法");
        }

        // 2. 构建分页SQL（MySQL语法）
        String sql = String.format(
                "SELECT * FROM %s LIMIT %d OFFSET %d",
                tableName,
                pageSize,
                (pageIndex - 1) * pageSize
        );

        // 3. 执行查询
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            // 如果结果为空，手动获取表结构
            if (result.isEmpty()) {
                return getTableColumnsAsEmptyRecord(tableName);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("查询表数据失败: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getTableDataBySql(String sqlStr, int pageIndex, int pageSize) {
        // 1. 参数校验
        if (pageIndex < 1 || pageSize < 1) {
            throw new IllegalArgumentException("分页参数不合法");
        }

        // 2. 构建分页SQL（MySQL语法）
        String sql = String.format(
                sqlStr +" LIMIT %d OFFSET %d",
                pageSize,
                (pageIndex - 1) * pageSize
        );

        // 3. 执行查询
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

            // 如果结果为空，手动获取表结构
            if (result.isEmpty()) {
                return getQueryColumnsAsEmptyRecord(sql);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("查询表数据失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> getQueryColumnsAsEmptyRecord(String sql) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> emptyRecord = new LinkedHashMap<>();

        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            // 获取结果集的元数据
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 遍历所有列
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i); // 获取列标签（如果有别名则返回别名）
                if (columnName == null) {
                    columnName = metaData.getColumnName(i); // 如果没有标签则获取列名
                }
                emptyRecord.put(columnName, null);
            }

            if (!emptyRecord.isEmpty()) {
                result.add(emptyRecord);
            }
        } catch (Exception e) {
            throw new RuntimeException("获取查询结构失败: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 获取表的列信息，并返回一个包含列名的空记录
     */
    private List<Map<String, Object>> getTableColumnsAsEmptyRecord(String tableName) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> emptyRecord = new LinkedHashMap<>();

        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // 获取表的列信息
            try (ResultSet columns = metaData.getColumns(
                    null,   // catalog (通常为 null 或 "")
                    null,   // schemaPattern (通常为 null 或 "")
                    tableName,  // tableName
                    null    // columnNamePattern (null 表示所有列)
            )) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    emptyRecord.put(columnName, null);  // 列名 -> null
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取表结构失败: " + e.getMessage(), e);
        }

        if (!emptyRecord.isEmpty()) {
            result.add(emptyRecord);
        }

        return result;
    }

    // 新增方法：获取总记录数
    @Override
    public long getTableCount(String tableName) {
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        try {
            return jdbcTemplate.queryForObject(countSql, Long.class);
        } catch (Exception e) {
            throw new RuntimeException("获取记录数失败: " + e.getMessage(), e);
        }
    }

    public long getQueryCount(String sql) {
        // 将原始SQL包装为COUNT查询
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") temp_count_table";
        try {
            return jdbcTemplate.queryForObject(countSql, Long.class);
        } catch (Exception e) {
            throw new RuntimeException("获取记录数失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<QueryResult> executeQuery(String sql) throws Exception {
        List<QueryResult> results = new ArrayList<>();

        // 简单实现，实际应处理多语句查询等
        QueryResult result = new QueryResult();
        try {
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
            result.setData(data);
            result.setSuccess(true);

            if (!data.isEmpty()) {
                result.setColumns(new ArrayList<>(data.get(0).keySet()));
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        results.add(result);

        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveTableData(String tableName, Map<String, List<Map<String, Object>>> changes) throws Exception {
        // 1. 参数校验
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }

        if (changes == null || changes.isEmpty()) {
            throw new IllegalArgumentException("变更数据不能为空");
        }

        // 2. 验证表名安全性
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("表名包含非法字符");
        }

        // 3. 获取三个操作数组
        List<Map<String, Object>> inserted = changes.get("inserted");
        List<Map<String, Object>> updated = changes.get("updated");
        List<Map<String, Object>> deleted = changes.get("deleted");

        // 4. 执行各操作
        int totalAffected = 0;

        // 处理新增数据
        if (inserted != null && !inserted.isEmpty()) {
            for (Map<String, Object> row : inserted) {
                totalAffected += executeInsert(tableName, row);
            }
        }

        // 处理更新数据
        if (updated != null && !updated.isEmpty()) {
            for (Map<String, Object> row : updated) {
                String idName = (tableName.contains("_")? tableName.split("_")[1] + "_":"") + "id";
                Object id = row.get((tableName.contains("_")? tableName.split("_")[1] + "_":"") + "id");
                if (id == null) {
                    id = row.get("id");
                    idName = "id";
                }
                if(id == null)
                    throw new IllegalArgumentException("更新操作必须提供id字段");
                // 移除id字段，因为它只用于WHERE条件
                Map<String, Object> data = new HashMap<>(row);
                data.remove(idName);
                totalAffected += executeUpdate(tableName, data, id, idName);
            }
        }

        // 处理删除数据
        if (deleted != null && !deleted.isEmpty()) {
            for (Map<String, Object> row : deleted) {
                Object id = row.get((tableName.contains("_")? tableName.split("_")[1] + "_":"") + "id");
                if (id == null) {
                    throw new IllegalArgumentException("删除操作必须提供id字段");
                }
                totalAffected += executeDelete(tableName, id);
            }
        }

        return totalAffected;
    }

    /**
     * 执行插入操作
     */
    private int executeInsert(String tableName, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        // 构建SQL
        String columns = String.join(", ", data.keySet());
        String placeholders = data.keySet().stream()
                .map(k -> "?")
                .collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName, columns, placeholders);

        // 执行更新
        return jdbcTemplate.update(sql, data.values().toArray());
    }

    /**
     * 执行更新操作
     */
    private int executeUpdate(String tableName, Map<String, Object> data, Object id, String idName) {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        // 构建SET部分
        String setClause = data.keySet().stream()
                .map(k -> k + " = ?")
                .collect(Collectors.joining(", "));
        // 构建完整SQL
        String sql = String.format("UPDATE %s SET %s WHERE " + idName + " = ?",
                tableName, setClause);

        // 合并参数值 (data.values + id)
        List<Object> params = new ArrayList<>(data.values());
        params.add(id);

        // 执行更新
        return jdbcTemplate.update(sql, params.toArray());
    }

    /**
     * 执行删除操作
     */
    private int executeDelete(String tableName, Object id) {
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        return jdbcTemplate.update(sql, id);
    }

    @Override
    public int deleteById(String tableName, Long id) {
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        try {
            return jdbcTemplate.update(sql, id);
        } catch (Exception e) {
            throw new RuntimeException("删除记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(String tableName, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String sql = "DELETE FROM " + tableName + " WHERE id IN (" +
                ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";

        try {
            return jdbcTemplate.update(sql, ids.toArray());
        } catch (Exception e) {
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }

    // 辅助方法：构建安全的SQL
    private String buildSafeSql(String baseSql, String tableName) {
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("非法的表名");
        }
        return baseSql.replace("{table}", tableName);
    }
}
