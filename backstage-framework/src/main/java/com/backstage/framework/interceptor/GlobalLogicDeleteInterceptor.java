package com.backstage.framework.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class GlobalLogicDeleteInterceptor implements Interceptor {

    // 匹配 FROM 或 JOIN 后面的 osh_ 开头表名（含可选别名）
    // 只匹配真正的表名，不匹配 SELECT 字段里的 osh_ 开头字段名
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?:FROM|JOIN)\\s+(osh_\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?",
            Pattern.CASE_INSENSITIVE
    );

    // 不含 delete_flag 字段的表名集合（拦截器跳过这些表，避免 Unknown column 错误）
    // 拼团 5 张表已统一补加 delete_flag 字段，排除列表为空
    private static final Set<String> TABLES_WITHOUT_DELETE_FLAG = Collections.emptySet();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = resolveStatementHandler(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(handler);
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();

        // 1. 只处理 SELECT
        if (!originalSql.trim().toUpperCase().startsWith("SELECT")) {
            return invocation.proceed();
        }

        // 2. 已有 delete_flag 条件，跳过（防止重复添加）
        if (originalSql.toLowerCase().contains("delete_flag")
                || originalSql.toLowerCase().contains("del_flag")) {
            return invocation.proceed();
        }
        // 3. 动态 SQL 未完成拼接，跳过
        String cleanSql = originalSql.trim().replaceAll("\\s+", " ");
        if (cleanSql.matches(".*\\bin\\s*$") || cleanSql.contains("#{")) {
            return invocation.proceed();
        }

        // 4. 找出 SQL 里所有 osh_ 表，收集需要加的条件
        StringBuilder conditions = new StringBuilder();
        String sqlForMatch = removeSubQueries(originalSql);
        Matcher matcher = TABLE_PATTERN.matcher(sqlForMatch);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            // 跳过没有 delete_flag 字段的表
            if (TABLES_WITHOUT_DELETE_FLAG.contains(tableName.toLowerCase())) {
                continue;
            }
            String alias = matcher.group(2);
            // alias 不能是 SQL 关键字（ON/SET/WHERE 等）
            if (alias != null && isSqlKeyword(alias)) {
                alias = null;
            }
            String prefix = (alias != null && !alias.isEmpty()) ? alias : tableName;
            String condition = prefix + ".delete_flag = 0";
            // 避免重复加同一个表的条件
            if (conditions.indexOf(condition) < 0) {
                if (conditions.length() > 0) conditions.append(" AND ");
                conditions.append(condition);
            }
        }

        if (conditions.length() == 0) {
            return invocation.proceed();
        }

        // 5. 直接在原始 SQL 上追加条件，不重新解析生成
        String newSql = appendConditions(originalSql, conditions.toString());
        metaObject.setValue("delegate.boundSql.sql", newSql);

        return invocation.proceed();
    }

    /**
     * 解开 MyBatis 插件和 JDK 动态代理包装，拿到真实的 StatementHandler。
     * 当前拦截器后续需要读取 delegate.boundSql；如果直接使用 invocation.getTarget()，
     * 在多插件叠加场景下拿到的可能只是代理对象，代理本身没有 delegate 属性，
     * 会触发 There is no getter for property named 'delegate' 异常。
     */
    private StatementHandler resolveStatementHandler(Object target) {
        MetaObject metaObject = SystemMetaObject.forObject(target);
        while (metaObject.hasGetter("h")) {
            Object object = metaObject.getValue("h");
            metaObject = SystemMetaObject.forObject(object);
        }
        while (metaObject.hasGetter("target")) {
            Object object = metaObject.getValue("target");
            metaObject = SystemMetaObject.forObject(object);
        }
        return (StatementHandler) metaObject.getOriginalObject();
    }

    /**
     * 将 SQL 中括号内的子查询内容替换为空格，避免匹配到子查询里的表名
     */
    private String removeSubQueries(String sql) {
        StringBuilder result = new StringBuilder(sql);
        int depth = 0;
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth > 0) {
                // 括号内的内容替换为空格，保持字符串长度不变（保证 insertPos 位置正确）
                result.setCharAt(i, ' ');
            }
        }
        return result.toString();
    }


    /**
     * 在原始 SQL 末尾追加条件
     * 有 WHERE：追加 AND (conditions)
     * 无 WHERE：追加 WHERE (conditions)
     * 有 ORDER BY / GROUP BY / LIMIT：插到它们前面
     */
    private String appendConditions(String sql, String conditions) {
        String upper = sql.toUpperCase();

        // 找插入位置：ORDER BY / GROUP BY / LIMIT / HAVING 之前，或末尾
        int insertPos = sql.length();
        for (String keyword : new String[]{"ORDER BY", "GROUP BY", "LIMIT", "HAVING"}) {
            int idx = upper.lastIndexOf(keyword);
            if (idx > 0 && idx < insertPos) {
                insertPos = idx;
            }
        }

        String before = sql.substring(0, insertPos).replaceAll("\\s+$", "");
        String after = sql.substring(insertPos);

        boolean hasWhere = upper.substring(0, insertPos).contains("WHERE");
        String connector = hasWhere ? " AND " : " WHERE ";

        return before + connector + "(" + conditions + ")" + (after.isEmpty() ? "" : " " + after.trim());
    }

    private boolean isSqlKeyword(String word) {
        String upper = word.toUpperCase();
        return upper.equals("ON") || upper.equals("SET") || upper.equals("WHERE")
                || upper.equals("JOIN") || upper.equals("LEFT") || upper.equals("RIGHT")
                || upper.equals("INNER") || upper.equals("OUTER") || upper.equals("AND")
                || upper.equals("OR") || upper.equals("NOT") || upper.equals("IN")
                || upper.equals("AS") || upper.equals("FROM") || upper.equals("SELECT")
                || upper.equals("ORDER") || upper.equals("GROUP") || upper.equals("HAVING")
                || upper.equals("LIMIT") || upper.equals("UNION") || upper.equals("BY");
    }
}
