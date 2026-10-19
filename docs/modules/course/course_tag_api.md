# 课程标签管理接口说明

## 一、新增标签接口

### 1. 接口信息
- **请求方式**: POST
- **完整路径**: `/system/course/tag`
- **认证要求**: 需要登录 + 权限 `system:course:tag:add`

### 2. 请求参数

**Header:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Body (JSON):**
```json
{
  "name": "人工智能",           // 必填，标签名称（唯一）
  "sort": 50,                  // 可选，排序权重（越大越靠前，默认 0）
  "status": 1,                 // 可选，状态（0-禁用 1-启用，默认 1）
  "remark": "AI 相关课程标签"    // 可选，备注说明
}
```

### 3. 响应示例

**成功响应:**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": 4  // 返回新创建的标签 ID
}
```

**失败响应（名称重复）:**
```json
{
  "code": 500,
  "msg": "标签名称已存在"
}
```

### 4. 测试命令

**使用 curl 测试:**
```bash
# 设置变量
TOKEN="your_token_here"
BASE_URL="http://localhost:8080"

# 新增标签
curl -X POST "${BASE_URL}/system/course/tag" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "name": "人工智能",
    "sort": 50,
    "status": 1,
    "remark": "AI 相关课程标签"
  }'
```

**使用 Postman/Apifox:**
1. 选择 POST 请求方法
2. 输入 URL: `http://localhost:8080/system/course/tag`
3. Headers 中添加:
   - `Content-Type: application/json`
   - `Authorization: Bearer {你的 token}`
4. Body 中选择 raw -> JSON，输入:
```json
{
  "name": "人工智能",
  "sort": 50,
  "status": 1,
  "remark": "AI 相关课程标签"
}
```

---

## 二、查询标签接口（已有）

### 1. 接口信息
- **请求方式**: GET
- **完整路径**: `/system/course/tags`
- **认证要求**: 无需登录（@Anonymous）

### 2. 请求参数

**Query 参数:**
- `keyword` (可选): 模糊查询关键字

### 3. 测试命令

**使用浏览器直接访问:**
```
http://localhost:8080/system/course/tags
http://localhost:8080/system/course/tags?keyword=开源
```

**使用 curl 测试:**
```bash
# 查询所有标签
curl "http://localhost:8080/system/course/tags"

# 带关键字查询
curl "http://localhost:8080/system/course/tags?keyword=AI"
```

**响应示例:**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "开源项目",
      "useCount": 10
    },
    {
      "id": 2,
      "name": "AI 企业刚需",
      "useCount": 5
    }
  ]
}
```

---

## 三、数据库初始化

执行 SQL 脚本创建标签表:

```bash
# 在 MySQL 中执行
mysql -u root -p your_database < sql/course_tag.sql
```

或直接在 MySQL 客户端中运行 `sql/course_tag.sql` 文件中的 SQL 语句。

---

## 四、注意事项

1. **权限配置**: 新增标签需要 `system:course:tag:add` 权限，需要在系统菜单管理中配置
2. **名称唯一性**: 标签名称不能重复，会进行唯一性校验
3. **事务支持**: 新增操作使用事务，失败会自动回滚
4. **默认排序**: 查询时按使用次数降序排列，使用次数相同时按 sort 字段降序
5. **逻辑删除**: 使用 `delete_flag` 字段进行逻辑删除，不物理删除数据
