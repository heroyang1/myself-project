package com.backstage.system.service.impl.bbs;

import com.backstage.common.core.controller.BaseController; // 用于调用 startPage
import com.backstage.common.core.domain.AjaxResult;
import com.backstage.common.core.domain.R;
import com.backstage.common.utils.DateUtils;
import com.backstage.common.utils.SecurityUtils;
import com.backstage.common.utils.ServletUtils;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.bbs.OshBbsPost;
import com.backstage.system.domain.dto.bbs.OshBbsCommentDto;
import com.backstage.system.domain.dto.bbs.OshBbsPostDto;
import com.backstage.system.domain.vo.bbs.*;
import com.backstage.system.mapper.bbs.OshBbsCommentMapper;
import com.backstage.system.mapper.bbs.OshBbsPostMapper;
import com.backstage.system.service.bbs.IOshBbsPostService;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper; // 直接用 PageHelper
import com.github.pagehelper.PageInfo;
import nonapi.io.github.classgraph.json.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OshBbsPostServiceImpl implements IOshBbsPostService {

    /** UTC ISO 时间格式（线程安全，可全局共享） */
    private static final DateTimeFormatter ISO_UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    @Autowired
    private OshBbsPostMapper postMapper;

    @Override
    public Map<String, Object> selectPostListVo(Long categoryId, Integer isTop) {
        // 1. 在 Service 层开启分页
        // 注意：若依的 startPage() 背后也是调用 PageHelper.startPage()
        // 这里直接获取请求中的 pageNum 和 pageSize
        Integer pageNum = ServletUtils.getParameterToInt("pageNum");
        Integer pageSize = ServletUtils.getParameterToInt("pageSize");

        // 如果前端没传，设置默认值
        int num = (pageNum == null) ? 1 : pageNum;
        int size = (pageSize == null) ? 10 : pageSize;

        PageHelper.startPage(num, size);

        // 2. 组装查询对象
        OshBbsPost query = new OshBbsPost();

        // 3. 获取当前登录用户 (处理未登录异常)
        Long loginUserId = null;
        try {
            loginUserId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 游客访问，保持 null 即可
        }

        // 4. 执行查询
        List<OshBbsPostDto> list = postMapper.selectPostList(query);
        List<BbsPostListVo> rows = new ArrayList<>();

        // 5. 循环转换 VO (逻辑保持不变)
        for (OshBbsPostDto p : list) {
            BbsPostListVo vo = new BbsPostListVo();
            vo.setId(p.getId());
            vo.setBbs_id(p.getCategoryId());
            vo.setUser_id(p.getUserId());
            vo.setComment_count(p.getCommentCount());
            vo.setSupport_count(p.getSupportCount());
            vo.setIs_top(p.getIsTop());
            // 找到报错的那一行，修改为：
            if (p.getCreateTime() != null) {
                vo.setCreated_time(DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", p.getCreateTime()));
            } else {
                vo.setCreated_time(""); // 或者设置为 "未知时间"
            }

            PostDescVo desc = new PostDescVo();
            desc.setText(p.getContent());
            if (StringUtils.isNotEmpty(p.getImages())) {
                desc.setImages(Arrays.asList(p.getImages().split(",")));
            } else {
                desc.setImages(new ArrayList<>());
            }
            vo.setDesc(desc);

            PostUserVo userVo = new PostUserVo();
            userVo.setId(p.getUserId());
            userVo.setName(p.getNickName());
            userVo.setAvatar(p.getAvatar());
            userVo.setSex("0".equals(p.getSex()) ? "男" : "1".equals(p.getSex()) ? "女" : "未知");
            vo.setUser(userVo);

            // 检查点赞
            vo.setIssupport(loginUserId != null && postMapper.checkIsSupport(p.getId(), loginUserId) > 0);

            rows.add(vo);
        }

        // 6. 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("count", new PageInfo(list).getTotal());
        return result;
    }

    @Override
    public Map<String, Object> selectPostByIdVo(Long id) {
        // 1. 获取当前登录用户
        Long loginUserId = null;
        try {
            loginUserId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 游客访问，保持 null 即可
        }

        // 2. 查询单条数据
        OshBbsPost query = new OshBbsPost();
        query.setId(id);
        List<OshBbsPostDto> list = postMapper.selectPostList(query);

        if (list == null || list.isEmpty()) {
            return null;
        }

        OshBbsPostDto p = list.get(0);

        // 3. 转换成 VO (这里建议把这块逻辑封装成一个 private 方法，因为列表和详情都在用)
        BbsPostListVo vo = new BbsPostListVo();
        vo.setId(p.getId());
        vo.setBbs_id(p.getCategoryId());
        vo.setUser_id(p.getUserId());
        vo.setComment_count(p.getCommentCount());
        vo.setSupport_count(p.getSupportCount());
        vo.setIs_top(p.getIsTop());
        vo.setCreated_time(p.getCreateTime() != null ?
                DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", p.getCreateTime()) : "");

        PostDescVo desc = new PostDescVo();
        desc.setText(p.getContent());
        desc.setImages(StringUtils.isNotEmpty(p.getImages()) ?
                Arrays.asList(p.getImages().split(",")) : new ArrayList<>());
        vo.setDesc(desc);

        PostUserVo userVo = new PostUserVo();
        userVo.setId(p.getUserId());
        userVo.setName(p.getNickName());
        userVo.setAvatar(p.getAvatar());
        userVo.setSex("0".equals(p.getSex()) ? "男" : "1".equals(p.getSex()) ? "女" : "未知");
        vo.setUser(userVo);

        vo.setIssupport(loginUserId != null && postMapper.checkIsSupport(p.getId(), loginUserId) > 0);

        // 4. 手动组装返回的 Map，避开 BeanUtils 报错
        Map<String, Object> result = new HashMap<>();
        result.put("id", vo.getId());
        result.put("bbs_id", vo.getBbs_id());
        result.put("user_id", vo.getUser_id());
        result.put("comment_count", vo.getComment_count());
        result.put("support_count", vo.getSupport_count());
        result.put("is_top", vo.getIs_top());
        result.put("created_time", vo.getCreated_time());
        result.put("desc", vo.getDesc());
        result.put("user", vo.getUser());
        result.put("issupport", vo.getIssupport());

        return result;
    }


    @Autowired
    private OshBbsCommentMapper commentMapper;

    @Override
    public Map<String, Object> selectCommentListVo(Long postId, Integer pageNum, Integer pageSize) {
        // 1. 分页查询主评论 (reply_id = 0)
        PageHelper.startPage(pageNum, pageSize);
        List<OshBbsCommentDto> mainComments = commentMapper.selectCommentList(postId, 0L);
        PageInfo<OshBbsCommentDto> pageInfo = new PageInfo<>(mainComments);

        List<PostCommentVo> rows = new ArrayList<>();
        for (OshBbsCommentDto dto : mainComments) {
            PostCommentVo vo = convertToCommentVo(dto);

            // 2. 查询对应的子评论 (不分页，全部查出)
            List<OshBbsCommentDto> children = commentMapper.selectCommentList(postId, dto.getId());
            List<PostCommentVo> childVos = new ArrayList<>();
            for (OshBbsCommentDto child : children) {
                childVos.add(convertToCommentVo(child));
            }
            vo.setPost_comments(childVos);
            rows.add(vo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", pageInfo.getTotal());
        result.put("rows", rows);
        return result;
    }

    @Override
    @Transactional // 涉及两张表的操作，必须开启事务
    public R supportPost(Long postId, Long userId) {
        // 1. 判断是否已经点赞过 (利用你之前的 checkIsSupport 方法)
        int count = postMapper.checkIsSupport(postId, userId);
        if (count > 0) {
            return R.fail(20000, "你已经点赞过了");
        }

        // 2. 插入点赞记录到 osh_bbs_support 表
        // 假设你有一个 OshBbsSupport 实体类，如果没有，可以直接用 Mapper 执行 SQL
        int rows = postMapper.insertSupport(postId, userId);

        if (rows > 0) {
            // 3. 帖子表点赞数 +1
            postMapper.incrementSupportCount(postId);
            return R.ok("ok");
        }

        return R.fail("点赞失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R unsupportPost(Long postId, Long userId) {
        // 1. 先查出这条帖子，看是否存在，以及是谁发的
        OshBbsPost post = postMapper.selectPostById(postId);
        if (post == null || post.getIsDelete() == 1) {
            return R.fail("帖子不存在或已被删除");
        }

        // 2. 权限校验：如果不是发帖人，也不是管理员（假设管理员ID为1），则拒绝
        if (!post.getUserId().equals(userId) && !userId.equals(1L)) {
            return R.fail("你没有权限删除他人的帖子");
        }

        // 3. 执行软删除 (更新 is_delete 状态)
        int rows = postMapper.decrementSupportCount(postId);
        if (rows > 0) {
            // 2. 只有我的记录被成功删除了，帖子的总赞数才减 1
            postMapper.deleteSupport(postId,1L);
            return R.ok("取消点赞成功");
        }

        return R.fail("删除失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R replyPost(Map<String, Object> params) {
        // 1. 解析 post_id (基础校验)
        Object postIdObj = params.get("post_id");
        if (postIdObj == null) return R.fail("帖子ID不能为空");
        Long postId = Long.valueOf(postIdObj.toString());

        // 2. 解析 content
        String content = (String) params.get("content");
        if (StringUtils.isEmpty(content)) return R.fail("评论内容不能为空");

        // 3. 处理回复逻辑 (解析嵌套的 reply_user)
        Long replyId = params.get("reply_id") != null ? Long.valueOf(params.get("reply_id").toString()) : 0L;
        Long replyUserId = 0L;

        // 关键点：解析 JSON 中的嵌套对象 {"id": 252, ...}
        if (params.get("reply_user") != null) {
            Object replyUserObj = params.get("reply_user");
            if (replyUserObj instanceof Map) {
                Map<String, Object> replyUserMap = (Map<String, Object>) replyUserObj;
                if (replyUserMap.get("id") != null) {
                    replyUserId = Long.valueOf(replyUserMap.get("id").toString());
                }
            }
        }

        // 4. 获取当前评论人 (假设你是写死的，或者从 Security 获取)
        Long userId = 1L; // 生产环境建议用 SecurityUtils.getUserId()

        // 5. 数据库操作：插入评论记录
        int insertRows = postMapper.insertComment(postId, content, replyId, replyUserId, userId);

        if (insertRows > 0) {
            // 6. 数据库操作：更新帖子主表的评论计数
            postMapper.incrementCommentCount(postId);
            return R.ok("评论成功");
        }

        return R.fail("评论发表失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deletePostById(Long id, Long loginUserId) {
        // 1. 基础校验
        if (id == null) return R.fail("帖子ID不能为空");

        // 2. 查出帖子原始信息
        OshBbsPost post = postMapper.selectPostById(id);
        if (post == null || post.getIsDelete() == 1) {
            return R.fail("帖子不存在或已被删除");
        }

        // 3. 权限校验：如果不是发帖人，也不是管理员（假设ID为1），则拒绝删除
        if (!post.getUserId().equals(loginUserId) && !loginUserId.equals(1L)) {
            return R.fail("你没有权限删除他人的帖子");
        }

        // 4. 执行软删除 (更新 is_delete = 1)
        int rows = postMapper.softDeletePost(id);

        if (rows > 0) {
            // 5. 联动清理：既然帖子删了，把该帖子的所有点赞关联记录也物理删除
            postMapper.deleteAllSupportByPostId(id);

            // 按照你提供的图片示例，成功返回 data: 1
            return R.ok(1);
        }

        return R.fail("删除失败");
    }

    @Override
    public R getMyPostList(Integer page, Long userId) {
        // 1. 分页逻辑
        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        // 2. 从数据库获取 DTO 列表
        int total = postMapper.selectMyPostCount(userId);
        List<OshBbsPostDto> dtos = postMapper.selectMyPostList(userId, offset, pageSize);

        // 3. 手动转换为 VO 列表 (匹配你要求的 JSON 嵌套结构)
        List<MyPostVO> rows = new ArrayList<>();
        if (dtos != null) {
            for (OshBbsPostDto dto : dtos) {
                MyPostVO vo = new MyPostVO();

                // 基础字段映射
                vo.setId(dto.getId());
                vo.setBbs_id(dto.getCategoryId()); // 对应文档的 bbs_id
                vo.setUser_id(dto.getUserId());
                vo.setComment_count(dto.getCommentCount());
                vo.setSupport_count(dto.getSupportCount());
                vo.setIs_top(dto.getIsTop());
                vo.setCreated_time(dto.getCreateTime());

                // 组装嵌套对象：desc (text 和 images 数组)
                PostDescVo descVo = new PostDescVo();
                descVo.setText(dto.getTitle()); // 假设标题是文本内容

                String imgStr = dto.getImages();
                List<String> imgList = new ArrayList<>();
                if (imgStr != null && !imgStr.isEmpty()) {
                    // 将数据库逗号分隔字符串转为 List<String>
                    String[] imgArray = imgStr.split(",");
                    for (String s : imgArray) {
                        imgList.add(s);
                    }
                }
                descVo.setImages(imgList);
                vo.setDesc(descVo);

                // 组装嵌套对象：user (id, name, avatar, sex)
                PostUserVo userVo = new PostUserVo();
                userVo.setId(dto.getUserId());
                userVo.setName(dto.getNickName()); // 使用 DTO 扩展的 nickName
                userVo.setAvatar(dto.getAvatar()); // 使用 DTO 扩展的 avatar

                // 处理性别显示
                String sexValue = "未知";
                if ("0".equals(dto.getSex())) sexValue = "男";
                else if ("1".equals(dto.getSex())) sexValue = "女";
                userVo.setSex(sexValue);

                vo.setUser(userVo);

                rows.add(vo);
            }
        }

        // 4. 组装最终返回的 data 结构
        Map<String, Object> data = new HashMap<>();
        data.put("count", total);
        data.put("rows", rows);

        // 5. 返回结果，确保 code 为 20000
        return R.ok(data);
    }

    @Autowired
    private ObjectMapper objectMapper; // Spring自带，用于对象转JSON字符串

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R savePost(Map<String, Object> params, Long userId) {
        // 1. 解析参数
        Object bbsIdObj = params.get("bbs_id");
        Object contentObj = params.get("content");

        if (bbsIdObj == null || contentObj == null) {
            return R.fail("参数错误：板块ID或内容不能为空");
        }

        Long bbsId = Long.valueOf(bbsIdObj.toString());
        List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;

        if (contentList.isEmpty()) {
            return R.fail("内容不能为空");
        }

        // 2. 处理 JSON 转换 (不使用第三方工具类，直接用Jackson)
        String contentStr = "";
        String descStr = "";
        try {
            contentStr = objectMapper.writeValueAsString(contentList);
//            descStr = objectMapper.writeValueAsString(contentList.get(0));
            // 改为只提取 text 文字内容：
            Map<String, Object> firstContent = contentList.get(0);
            if (firstContent.get("text") != null) {
                descStr = firstContent.get("text").toString();
                // 如果文字太长，截取前50个字防止再次溢出
                if (descStr.length() > 50) {
                    descStr = descStr.substring(0, 50) + "...";
                }
            }
        } catch (Exception e) {
            return R.fail("JSON解析失败");
        }

        // 3. 构造并保存实体
        OshBbsPost post = new OshBbsPost();
        post.setCategoryId(bbsId);
        post.setContent(contentStr);
        post.setTitle(descStr); // 对应数据库中存放desc的字段
        post.setUserId(userId);
        post.setCommentCount(0);
        post.setSupportCount(0);
        post.setIsTop(0);
        post.setCreateTime(new Date());
        post.setIsDelete(0);

        int rows = postMapper.insertPost(post);

        if (rows > 0) {
            // 4. 组装返回数据 (完全对齐你要求的格式)
            Map<String, Object> data = new HashMap<>();
            data.put("id", post.getId()); // 注意：需要在Mapper开启主键回填
            data.put("bbs_id", bbsId);
            data.put("content", contentStr);
            data.put("desc", descStr);
            data.put("user_id", userId);
            data.put("userId", userId);
            data.put("comment_count", 0);
            data.put("support_count", 0);
            data.put("is_top", 0);
            data.put("school_id", 11);

            // 格式化时间为：2021-05-29T09:24:54.074Z
            String timeStr = ISO_UTC_FORMATTER.format(post.getCreateTime().toInstant());

            data.put("created_time", timeStr);
            data.put("updated_time", timeStr);

            // 确保返回的 code 是 20000
            R r = R.ok(data);
            return r;
        }

        return R.fail("发布失败");
    }

    private PostCommentVo convertToCommentVo(OshBbsCommentDto dto) {
        PostCommentVo vo = new PostCommentVo();
        vo.setId(dto.getId());
        vo.setContent(dto.getContent());
        vo.setReply_id(dto.getReplyId());
        vo.setIs_top(dto.getIsTop());
        vo.setCreated_time(dto.getCreateTime() != null ?
                DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", dto.getCreateTime()) : "");

        PostUserVo userVo = new PostUserVo();
        userVo.setId(dto.getUserId());
        userVo.setName(dto.getNickName());
        userVo.setAvatar(dto.getAvatar());
        vo.setUser(userVo);
        return vo;
    }



}