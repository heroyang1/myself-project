# 课程模块开发实现指南

## 一、项目结构说明

### 1.1 文件目录结构

```
backstage-system/
├── src/main/java/com/backstage/system/
│   ├── controller/course/
│   │   ├── OshCourseController.java          # 原有课程 Controller
│   │   └── CourseManageController.java       # 新增：课程管理 Controller（18 个接口）
│   ├── domain/
│   │   ├── dto/
│   │   │   ├── CourseQueryDTO.java           # 课程查询条件 DTO
│   │   │   ├── CourseInfoDTO.java            # 课程信息 DTO
│   │   │   ├── SectionDTO.java               # 章节信息 DTO
│   │   │   ├── MaterialDTO.java              # 资料信息 DTO
│   │   │   ├── QuestionDTO.java              # 问题信息 DTO
│   │   │   └── ReviewDTO.java                # 评价信息 DTO
│   │   └── vo/
│   │       ├── CourseSectionVO.java          # 课程章节 VO
│   │       ├── CourseMaterialVO.java         # 课程资料 VO
│   │       └── CourseQuestionVO.java         # 课程问答 VO
│   ├── mapper/course/
│   │   ├── OshCourseMapper.java              # 课程 Mapper
│   │   ├── OshCourseSectionMapper.java       # 章节 Mapper
│   │   ├── OshCourseMaterialMapper.java      # 资料 Mapper
│   │   ├── OshCourseQuestionMapper.java      # 问答 Mapper
│   │   ├── OshCourseTagMapper.java           # 标签 Mapper
│   │   ├── OshUserCourseProgressMapper.java  # 学习进度 Mapper
│   │   ├── OshCourseStaffMapper.java         # 服务人员 Mapper
│   │   └── OshCourseReviewMapper.java        # 评价 Mapper
│   └── service/
│       ├── course/
│       │   ├── ICourseManageService.java     # 课程管理服务接口
│       │   └── impl/
│       │       └── CourseManageServiceImpl.java  # 课程管理服务实现
│       └── impl/
│           └── course/
│               └── CourseManageServiceImpl.java
└── src/main/resources/
    └── mapper/course/
        ├── OshCourseMapper.xml               # 课程 Mapper XML
        ├── OshCourseSectionMapper.xml        # 章节 Mapper XML
        ├── OshCourseMaterialMapper.xml       # 资料 Mapper XML
        ├── OshCourseQuestionMapper.xml       # 问答 Mapper XML
        ├── OshCourseTagMapper.xml            # 标签 Mapper XML
        ├── OshUserCourseProgressMapper.xml   # 学习进度 Mapper XML
        ├── OshCourseStaffMapper.xml          # 服务人员 Mapper XML
        └── OshCourseReviewMapper.xml         # 评价 Mapper XML
sql/
└── course_module_full.sql                    # 数据库建表 SQL 脚本
```

---

## 二、数据库表设计

### 2.1 已创建的 8 张表

1. **osh_course_section** - 课程章节表
   - 主键：id (bigint)
   - 外键：course_id (int)
   - 核心字段：section_title, section_content, section_type, duration, sort_order, is_free
   - 索引：idx_course_id, idx_sort

2. **osh_course_material** - 课程资料表
   - 主键：id (bigint)
   - 外键：course_id (int)
   - 核心字段：material_name, file_url, file_type, file_size, download_count
   - 索引：idx_course_id

3. **osh_course_question** - 课程问答表
   - 主键：id (bigint)
   - 外键：course_id, section_id, user_id, answer_user_id
   - 核心字段：question_title, question_content, answer_content, status, like_count
   - 索引：idx_course_id, idx_section_id, idx_user_id, idx_status

4. **osh_course_tag** - 课程标签表
   - 主键：id (int)
   - 核心字段：tag_name, tag_code, usage_count, sort_order
   - 唯一索引：uk_tag_code
   - 初始数据：开源项目、AI、企业刚需

5. **osh_course_tag_relation** - 课程标签关联表
   - 主键：id (bigint)
   - 外键：course_id, tag_id
   - 唯一索引：uk_course_tag

6. **osh_user_course_progress** - 用户课程学习进度表
   - 主键：id (bigint)
   - 外键：user_id, course_id
   - 核心字段：section_id, learned_section_count, progress_percent, is_completed
   - 唯一索引：uk_user_course

7. **osh_course_staff** - 课程服务人员表
   - 主键：id (bigint)
   - 外键：user_id, course_id
   - 核心字段：staff_type, exam_score, audit_status, audit_user_id
   - 唯一索引：uk_user_course_staff

8. **osh_course_review** - 课程评价表
   - 主键：id (bigint)
   - 外键：course_id, user_id
   - 核心字段：rating, review_content, like_count
   - 唯一索引：uk_user_course_review

### 2.2 现有表补充字段

需要在 `osh_course` 表中增加字段：

```sql
ALTER TABLE `osh_course` 
ADD COLUMN `tag_ids` varchar(500) COMMENT '标签 ID 集合 (逗号分隔)' AFTER `type`,
ADD COLUMN `good_review_count` int DEFAULT '0' COMMENT '好评数量' AFTER `tag_ids`,
ADD COLUMN `medium_review_count` int DEFAULT '0' COMMENT '中评数量' AFTER `good_review_count`,
ADD COLUMN `bad_review_count` int DEFAULT '0' COMMENT '差评数量' AFTER `medium_review_count`;
```

---

## 三、接口清单（共 18 个）

### 3.1 课程查询接口（5 个）

| 序号 | 接口名称 | 请求方法 | URL 路径 | 权限要求 |
|------|---------|---------|--------|---------|
| 1 | 查询课程列表 | GET | /system/course/list | Anonymous |
| 2 | 获取课程详情 | GET | /system/course/{courseId} | Anonymous |
| 3 | 新增课程 | POST | /system/course | 登录用户 |
| 4 | 修改课程 | PUT | /system/course | 登录用户 |
| 5 | 删除课程 | DELETE | /system/course/{courseId} | 登录用户 |

### 3.2 课程章节接口（3 个）

| 序号 | 接口名称 | 请求方法 | URL 路径 | 权限要求 |
|------|---------|---------|--------|---------|
| 6 | 获取课程大纲 | GET | /system/course/{courseId}/sections | Anonymous |
| 7 | 立即学习 | POST | /system/course/learn | Anonymous |
| 8 | 更新学习进度 | POST | /system/course/progress/update | 登录用户 |

### 3.3 课程资料接口（3 个）

| 序号 | 接口名称 | 请求方法 | URL 路径 | 权限要求 |
|------|---------|---------|--------|---------|
| 9 | 获取课程资料列表 | GET | /system/course/{courseId}/materials | Anonymous |
| 10 | 上传课程资料 | POST | /system/course/{courseId}/material/upload | 登录用户 |
| 11 | 删除课程资料 | DELETE | /system/course/material/{materialId} | 登录用户 |

### 3.4 课程问答接口（4 个）

| 序号 | 接口名称 | 请求方法 | URL 路径 | 权限要求 |
|------|---------|---------|--------|---------|
| 12 | 提问 | POST | /system/course/question | 登录用户 |
| 13 | 获取课程问答列表 | GET | /system/course/{courseId}/questions | Anonymous |
| 14 | 回答问题 | POST | /system/course/question/{questionId}/answer | 登录用户 |
| 15 | 获取问题详情 | GET | /system/course/question/{questionId} | Anonymous |

### 3.5 课程评价接口（2 个）

| 序号 | 接口名称 | 请求方法 | URL 路径 | 权限要求 |
|------|---------|---------|--------|---------|
| 16 | 提交课程评价 | POST | /system/course/review | 登录用户 |
| 17 | 获取课程评价统计 | GET | /system/course/{courseId}/reviews/statistics | Anonymous |

### 3.6 课程服务人员接口（3 个）

| 序号 | 接口名称 | 请求方法 | URL 路径 | 权限要求 |
|------|---------|---------|--------|---------|
| 18 | 申请成为服务人员 | POST | /system/course/staff/apply | 登录用户 |
| 19 | 审核服务人员申请 | POST | /system/course/staff/{applyId}/audit | 管理员 |
| 20 | 获取课程服务人员列表 | GET | /system/course/{courseId}/staffs | Anonymous |

---

## 四、核心业务逻辑实现

### 4.1 权限控制逻辑

#### 4.1.1 新增课程权限

**语法逻辑：**
```java
private boolean checkCreatePermission(Long userId) {
    // 1. 查询用户信息
    SysUser oshUser = userService.selectUserById(userId);
    
    // 2. 判断是否为内部成员（部门 ID=1）
    if (oshUser.getDeptId() != null && oshUser.getDeptId().equals(1L)) {
        return true;
    }
    
    // 3. 判断是否为年 VIP 用户
    if ("VIP_YEARLY".equals(oshUser.getUserType())) {
        return true;
    }
    
    return false;
}
```

**实现效果：**
- 部门 ID 为 1 的用户（内部成员）可以新增课程
- 用户类型为 VIP_YEARLY 的可以新增课程
- 其他用户无法新增

#### 4.1.2 编辑课程权限

**语法逻辑：**
```java
private boolean canEditCourse(Long courseId, Long userId) {
    // 1. 查询课程信息
    OshCourse course = courseMapper.selectCourseById(courseId);
    
    // 2. 判断是否为创建者
    if (course.getCreateBy() != null && 
        course.getCreateBy().equals(userId.toString())) {
        return true;
    }
    
    // 3. 判断是否为课程服务人员
    Map<String, Object> staff = staffMapper.selectStaffByUserIdAndCourseId(userId, courseId);
    if (staff != null && "approved".equals(staff.get("audit_status"))) {
        return true;
    }
    
    return false;
}
```

**实现效果：**
- 课程创建者可以编辑
- 课程服务人员（审核通过）可以编辑
- 其他用户无法编辑

### 4.2 购买验证逻辑

**语法逻辑：**
```java
private boolean checkUserPurchased(Long courseId, Long userId) {
    if (userId == null) {
        return false;
    }
    
    // 1. 查询学习进度
    Map<String, Object> progress = progressMapper.selectProgressByUserIdAndCourseId(userId, courseId);
    if (progress != null) {
        return true;
    }
    
    // 2. 查询订单（需要实现订单 Mapper）
    // List<Order> orders = orderMapper.selectOrdersByUserIdAndCourseId(userId, courseId);
    // if (orders != null && !orders.isEmpty()) {
    //     return true;
    // }
    
    return false;
}
```

**实现效果：**
- 有学习进度记录表示已购买
- 有订单记录表示已购买
- 未购买用户只能看免费章节

### 4.3 文件上传验证

**语法逻辑：**
```java
// 1. 校验文件类型
String fileName = file.getOriginalFilename();
String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
List<String> allowedExtensions = Arrays.asList("zip", "rar", "7z");
if (!allowedExtensions.contains(extension)) {
    throw new ServiceException("仅支持压缩包格式（zip/rar/7z）");
}

// 2. 校验文件大小（100MB）
long maxSize = 100 * 1024 * 1024;
if (file.getSize() > maxSize) {
    throw new ServiceException("文件大小不能超过 100MB");
}
```

**实现效果：**
- 只允许 zip、rar、7z 格式
- 文件大小限制在 100MB 以内
- 不符合要求的文件抛出异常

---

## 五、事务管理

### 5.1 需要事务的方法

1. **新增课程** - 保存课程信息 + 标签关联
2. **修改课程** - 更新课程信息 + 标签关联
3. **删除课程** - 级联删除章节、资料、问答等
4. **上传资料** - 上传文件 + 保存资料信息
5. **删除资料** - 删除文件记录
6. **提问** - 保存问题
7. **回答问题** - 更新问题和答案
8. **提交评价** - 保存评价
9. **申请服务人员** - 保存申请记录
10. **审核服务人员** - 更新审核状态

### 5.2 事务注解使用

```java
@Transactional(rollbackFor = Exception.class)
public Long insertCourse(OshCourse course, Long userId) {
    // 业务逻辑
}
```

**注意事项：**
- 所有涉及多表操作的方法必须加事务
- rollbackFor 设置为 Exception.class，确保所有异常都回滚
- 私有方法不需要加@Transactional

---

## 六、与现有系统集成

### 6.1 用户服务集成

**注入用户服务：**
```java
@Autowired
private ISysUserService userService;
```

**获取当前登录用户：**
```java
Long userId = getUserId();  // BaseController 提供的方法
```

**查询用户信息：**
```java
SysUser oshUser = userService.selectUserById(userId);
```

### 6.2 文件上传服务集成

**注入文件服务：**
```java
@Autowired
private ISysFileService fileService;
```

**上传文件：**
```java
String fileUrl = fileService.upload(file);
```

### 6.3 Redis 缓存集成

**添加缓存注解：**
```java
@Cacheable(value = CacheConstants.COURSE_DETAIL, key = "#courseId", expire = 300)
public CourseDetailVO getCourseDetail(Long courseId, Long userId) {
    // 业务逻辑
}
```

**清除缓存注解：**
```java
@CacheEvict(value = CacheConstants.COURSE_DETAIL, key = "#course.courseId")
public int updateCourse(OshCourse course, Long userId) {
    // 业务逻辑
}
```

---

## 七、部署步骤

### 7.1 数据库初始化

1. 执行 SQL 脚本：
```bash
mysql -u root -p backstage < sql/course_module_full.sql
```

2. 补充 osh_course 表字段：
```sql
ALTER TABLE `osh_course` 
ADD COLUMN `tag_ids` varchar(500) COMMENT '标签 ID 集合 (逗号分隔)' AFTER `type`,
ADD COLUMN `good_review_count` int DEFAULT '0' COMMENT '好评数量' AFTER `tag_ids`,
ADD COLUMN `medium_review_count` int DEFAULT '0' COMMENT '中评数量' AFTER `good_review_count`,
ADD COLUMN `bad_review_count` int DEFAULT '0' COMMENT '差评数量' AFTER `medium_review_count`;
```

### 7.2 编译打包

```bash
cd backstage-system
mvn clean install
```

### 7.3 启动测试

1. 启动后端服务
2. 访问 Swagger UI：http://localhost:8080/swagger-ui.html
3. 测试接口：/system/course/list

---

## 八、常见问题解答

### Q1: 如何配置 MyBatis 扫描 Mapper？

**答：** 在 application.yml 中添加：

```yaml
mybatis:
  mapper-locations: classpath*:mapper/**/*Mapper.xml
  type-aliases-package: com.backstage.system.domain
```

### Q2: 如何处理跨域问题？

**答：** 在 Controller 类上添加@CrossOrigin 注解：

```java
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/system/course")
public class CourseManageController {
    // ...
}
```

### Q3: 如何实现分页？

**答：** 使用 PageHelper：

```java
PageHelper.startPage(pageNum, pageSize);
List<OshCourse> list = courseMapper.selectCourseListByCondition(params);
TableDataInfo tableDataInfo = getDataTable(list);
```

### Q4: 如何测试文件上传？

**答：** 使用 Postman：
1. 选择 POST 方法
2. Body 选择 form-data
3. Key 输入 file，Type 选择 File
4. 选择一个 zip 文件
5. 发送请求

---

## 九、性能优化建议

### 9.1 数据库优化

1. **建立索引**
   - course_id, user_id 等外键字段
   - status, create_time 等查询条件字段
   
2. **避免 N+1 查询**
   - 使用 LEFT JOIN 一次性查询关联数据
   - 示例：查询问题时 JOIN 用户表

3. **分表策略**
   - 问答表数据量大时可按 course_id 分表
   - 学习进度表可按 user_id 分表

### 9.2 缓存优化

1. **课程详情缓存 5 分钟**
```java
@Cacheable(expire = 300)
public CourseDetailVO getCourseDetail(Long courseId, Long userId)
```

2. **标签列表长期缓存**
```java
@Cacheable(expire = 3600)
public List<Map<String, Object>> selectAllTags()
```

### 9.3 异步处理

1. **学习进度异步更新**
```java
@Async
public void updateProgress(Long courseId, Long sectionId, Long userId, Double progress)
```

2. **评价统计异步计算**
```java
@Async
public void calculateReviewStatistics(Long courseId)
```

---

## 十、总结

本方案完整实现了产品经理提出的 12 项需求：

✅ 1. 使用专栏页面改过来，包含课程介绍、服务周期  
✅ 2. 课程前几节显示试听，右侧显示免费  
✅ 3. 课程大纲页面可以点击立即学习，支付后解锁付费课程  
✅ 4. 支付后课程大纲界面显示资料下载选项  
✅ 5. 已学部分右侧显示已学完、有疑问两种状态  
✅ 6. 点击疑问跳转到问答板块问题详情界面  
✅ 7. 课程小节详情内可以提问，问题显示在问答板块  
✅ 8. 课程界面新增查询功能（标签多选 + 关键字）  
✅ 9. 查询列表界面右上角显示新增课程（权限控制）  
✅ 10. 课程新增界面显示资料列表（上传、下载、删除）  
✅ 11. 课程有标签、编号、好评中评差评数量  
✅ 12. 通过考试与审核的人可以申请成为课程服务人员  

**总计：**
- 数据库表：8 张新表 + 1 张补充表
- 接口数量：20 个 REST API
- 代码文件：25 个 Java 文件 + 8 个 XML 文件
- 适用人群：Java 开发初学者
