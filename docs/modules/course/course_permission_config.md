# 课程模块权限配置说明

## 一、权限配置总览

### 1.1 新增的权限标识

| 权限标识 | 对应接口 | 说明 |
|---------|---------|------|
| `system:course:add` | 新增课程 | 仅内部成员或年 VIP 用户 |
| `system:course:edit` | 修改课程 | 课程创建者或服务人员 |
| `system:course:remove` | 删除课程 | 课程创建者或服务人员 |
| `system:course:material:upload` | 上传资料 | 课程创建者或服务人员 |
| `system:course:material:delete` | 删除资料 | 课程创建者或服务人员 |
| `system:course:question:answer` | 回答问题 | 课程服务人员 |
| `system:course:staff:audit` | 审核服务人员 | 管理员 |

---

## 二、数据库配置步骤

### 2.1 在 sys_menu 表中添加菜单和按钮权限

#### 1. 添加课程管理菜单（如果还没有）

```sql
-- 查询父级菜单ID（系统管理）
SELECT menu_id FROM sys_menu WHERE menu_name = '系统管理' AND parent_id = 0;

-- 假设系统管理的 menu_id 为 1
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('课程管理', 1, 5, 'course', 'course/index', 1, 0, 'C', '0', '0', NULL, 'book', 'admin', NOW(), '课程管理菜单');

-- 获取刚插入的 menu_id（假设为 100）
SELECT LAST_INSERT_ID();
```

#### 2. 添加增删改查按钮权限

```sql
-- 假设课程管理菜单的 menu_id 为 100

-- 新增课程按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('课程新增', 100, 1, '', '', 1, 0, 'F', '0', '0', 'system:course:add', '#', 'admin', NOW(), '');

-- 修改课程按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('课程修改', 100, 2, '', '', 1, 0, 'F', '0', '0', 'system:course:edit', '#', 'admin', NOW(), '');

-- 删除课程按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('课程删除', 100, 3, '', '', 1, 0, 'F', '0', '0', 'system:course:remove', '#', 'admin', NOW(), '');

-- 上传资料按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('资料上传', 100, 4, '', '', 1, 0, 'F', '0', '0', 'system:course:material:upload', '#', 'admin', NOW(), '');

-- 删除资料按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('资料删除', 100, 5, '', '', 1, 0, 'F', '0', '0', 'system:course:material:delete', '#', 'admin', NOW(), '');

-- 回答问题按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('问题回答', 100, 6, '', '', 1, 0, 'F', '0', '0', 'system:course:question:answer', '#', 'admin', NOW(), '');

-- 审核服务人员按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('服务人员审核', 100, 7, '', '', 1, 0, 'F', '0', '0', 'system:course:staff:audit', '#', 'admin', NOW(), '');
```

---

## 三、角色权限分配

### 3.1 为不同角色分配权限

#### 1. 超级管理员（admin）- 拥有所有权限

```sql
-- 查询 admin 角色的 role_id
SELECT role_id FROM sys_role WHERE role_key = 'admin';

-- 假设 admin 的 role_id 为 1
-- 将课程管理菜单和所有按钮权限分配给 admin
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE perms IN (
    'system:course:add',
    'system:course:edit',
    'system:course:remove',
    'system:course:material:upload',
    'system:course:material:delete',
    'system:course:question:answer',
    'system:course:staff:audit'
);
```

#### 2. 普通管理员 - 拥有部分权限

```sql
-- 查询普通管理员角色的 role_id
SELECT role_id FROM sys_role WHERE role_key = 'common';

-- 假设普通管理员的 role_id 为 2
-- 分配除审核外的所有权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE perms IN (
    'system:course:add',
    'system:course:edit',
    'system:course:remove',
    'system:course:material:upload',
    'system:course:material:delete',
    'system:course:question:answer'
);
```

#### 3. 课程服务人员 - 仅回答问题权限

```sql
-- 如果有专门的服务人员角色
SELECT role_id FROM sys_role WHERE role_key = 'staff';

-- 假设服务人员的 role_id 为 3
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE perms = 'system:course:question:answer';
```

---

## 四、用户角色分配

### 4.1 为用户分配角色

```sql
-- 为用户 1（admin）分配管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 为用户 2 分配普通管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 2);

-- 为用户 3 分配服务人员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (3, 3);
```

---

## 五、前端界面权限控制

### 5.1 使用 v-hasPermi 指令控制按钮显示

```vue
<!-- 新增课程按钮 -->
<el-button
  type="primary"
  plain
  icon="Plus"
  v-hasPermi="['system:course:add']"
  @click="handleAdd"
>新增</el-button>

<!-- 修改课程按钮 -->
<el-button
  type="primary"
  plain
  icon="Edit"
  v-hasPermi="['system:course:edit']"
  @click="handleUpdate"
  :disabled="single"
>修改</el-button>

<!-- 删除课程按钮 -->
<el-button
  type="danger"
  plain
  icon="Delete"
  v-hasPermi="['system:course:remove']"
  @click="handleDelete"
  :disabled="multiple"
>删除</el-button>

<!-- 上传资料按钮 -->
<el-button
  type="primary"
  plain
  icon="Upload"
  v-hasPermi="['system:course:material:upload']"
  @click="handleUpload"
>上传</el-button>

<!-- 删除资料按钮 -->
<el-button
  type="danger"
  plain
  icon="Delete"
  v-hasPermi="['system:course:material:delete']"
  @click="handleDeleteMaterial"
>删除</el-button>

<!-- 回答问题按钮 -->
<el-button
  type="primary"
  plain
  icon="Comment"
  v-hasPermi="['system:course:question:answer']"
  @click="handleAnswer"
>回答</el-button>

<!-- 审核服务人员按钮 -->
<el-button
  type="success"
  plain
  icon="Check"
  v-hasPermi="['system:course:staff:audit']"
  @click="handleAudit"
>审核</el-button>
```

---

## 六、权限验证流程

### 6.1 请求处理流程

```
用户发起请求
    ↓
Shiro 拦截器检查@RequiresPermissions 注解
    ↓
从当前登录用户获取权限列表
    ↓
验证是否包含所需权限
    ↓
┌──────────────┬──────────────┐
│   有权限     │   无权限     │
└──────────────┴──────────────┘
    ↓                ↓
执行 Controller    抛出 UnauthorizedException
    ↓                ↓
返回成功结果      Shiro 异常处理器返回 403
```

### 6.2 权限验证代码位置

**文件路径：** 
- `E:\桌面\judge-osh\backstage-framework\src\main\java\com\backstage\framework\aspectj\AuthorizationAspect.java`

**核心逻辑：**
```java
@Around("@annotation(requiresPermissions)")
public Object around(ProceedingJoinPoint point, RequiresPermissions requiresPermissions) throws Throwable {
    // 获取当前用户
    Subject subject = SecurityUtils.getSubject();
    
    // 验证权限
    if (!subject.isPermitted(requiresPermissions.value())) {
        throw new AuthorizationException("无权限访问");
    }
    
    // 执行方法
    return point.proceed();
}
```

---

## 七、测试权限配置

### 7.1 使用 Postman 测试

#### 测试用例 1：有权限新增课程

```http
POST http://localhost:8080/system/course
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "测试课程",
  "price": 99.00
}
```

**预期结果：** 返回 200，新增成功

#### 测试用例 2：无权限新增课程

```http
POST http://localhost:8080/system/course
Content-Type: application/json
Authorization: Bearer {token_without_permission}

{
  "title": "测试课程",
  "price": 99.00
}
```

**预期结果：** 返回 403 Forbidden

---

## 八、常见问题

### Q1: 添加了权限但还是提示无权限？

**答：** 检查以下几点：
1. 权限字符串是否完全一致（区分大小写）
2. 用户是否有对应的角色
3. 角色是否有关联菜单权限
4. 清除 Redis 缓存中的用户权限信息

### Q2: 如何快速测试权限配置？

**答：** 直接给用户分配 super_admin 角色，该角色默认拥有所有权限

### Q3: 动态权限如何控制？

**答：** 使用 `@DataScope` 注解实现数据权限，例如只能操作自己创建的课程

```java
@DataScope(deptAlias = "d", userAlias = "u")
public List<OshCourse> selectCourseList(OshCourse course);
```

---

## 九、优化建议

### 9.1 权限命名规范

- 格式：`模块：业务：操作`
- 示例：`system:course:add`
- 全小写，下划线分隔

### 9.2 权限粒度控制

- **粗粒度**：菜单级别权限（查看页面）
- **细粒度**：按钮级别权限（增删改操作）
- **数据级**：行级别权限（只能操作自己的数据）

### 9.3 性能优化

- 用户权限缓存到 Redis，避免频繁查询数据库
- 缓存过期时间设置为 30 分钟
- 权限变更时主动清除缓存

---

## 十、总结

通过使用 `@RequiresPermissions`注解：

✅ **优点：**
- 权限校验与业务逻辑分离
- 代码更简洁，易于维护
- 符合若依框架规范
- 支持灵活的权限配置

⚠️ **注意事项：**
- 必须在 sys_menu 表中正确配置权限
- 用户必须分配对应的角色
- 注意清理缓存避免权限不更新
- 敏感操作建议增加二次确认
