package com.backstage.system.mapper.servergroup;

import com.backstage.system.domain.servergroup.OshGroupUserInitiated;
import com.backstage.system.domain.vo.UserInitiatedActivityListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户发起拼团Mapper接口
 * 
 * @author system
 * @date 2026-05-05
 */
@Mapper
public interface OshGroupUserInitiatedMapper {
    
    /**
     * 插入用户发起拼团记录
     * 
     * @param initiated 发起记录
     * @return 影响行数
     */
    int insertUserInitiated(OshGroupUserInitiated initiated);
    
    /**
     * 根据ID查询用户发起拼团记录
     * 
     * @param id 发起记录ID
     * @return 发起记录
     */
    OshGroupUserInitiated selectById(@Param("id") Long id);

    /** 参团创建事务内锁定用户发起记录，串行化最后名额校验。 */
    OshGroupUserInitiated selectByIdForUpdate(@Param("id") Long id);
    
    /**
     * 查询用户发起的拼团列表
     * 
     * @param userId 用户ID
     * @param groupStatus 组团状态筛选（可选）
     * @return 发起的拼团列表
     */
    List<OshGroupUserInitiated> selectByUserId(@Param("userId") Long userId, @Param("groupStatus") Integer groupStatus);
    
    /**
     * 查询用户发起拼团活动列表（C端展示用）
     * 
     * @param status 状态筛选（可选）
     * @return 用户发起拼团活动列表
     */
    List<UserInitiatedActivityListVO> selectUserInitiatedActivityList(@Param("status") Integer status, @Param("type") String type);
    
    /**
     * 更新发起拼团记录
     * 
     * @param initiated 发起记录
     * @return 影响行数
     */
    int updateUserInitiated(OshGroupUserInitiated initiated);
    
    /**
     * 更新当前参团人数
     * 
     * @param id 发起记录ID
     * @param currentNum 当前人数
     * @return 影响行数
     */
    int updateCurrentNum(@Param("id") Long id, @Param("currentNum") Integer currentNum);
    
    /**
     * 更新组团状态
     * 
     * @param id 发起记录ID
     * @param groupStatus 组团状态
     * @return 影响行数
     */
    int updateGroupStatus(@Param("id") Long id, @Param("groupStatus") Integer groupStatus);
    
    /**
     * 更新服务器时间
     * 
     * @param id 发起记录ID
     * @param serverStartTime 服务器开始时间
     * @param serverExpireTime 服务器到期时间
     * @return 影响行数
     */
    int updateServerTime(@Param("id") Long id, @Param("serverStartTime") LocalDateTime serverStartTime, @Param("serverExpireTime") LocalDateTime serverExpireTime);
    
    /**
     * 检查用户是否已参与该拼团
     * 
     * @param activityId 拼团活动ID
     * @param userId 用户ID
     * @return 参与记录数
     */
    int checkUserAlreadyJoined(@Param("activityId") Long activityId, @Param("userId") Long userId);
    
    /**
     * 查询拼团活动详情（用于校验）
     * 
     * @param activityId 拼团活动ID
     * @return 拼团活动信息（包含当前人数、最大人数、状态等）
     */
    @MapKey("id")
    java.util.Map<String, Object> selectActivityDetail(@Param("activityId") Long activityId);

    // ==================== 定时任务相关方法 ====================

    /**
     * 查询已过招募期且状态仍为招募中的用户发起拼团记录
     *
     * @param expireBefore 过期截止时间
     * @return 过期的发起记录列表
     */
    List<OshGroupUserInitiated> selectExpiredInitiatedGroups(@Param("expireBefore") LocalDateTime expireBefore);

    /**
     * 批量更新用户发起拼团记录的状态
     *
     * @param ids         记录ID列表
     * @param groupStatus 目标状态
     * @return 影响行数
     */
    int batchUpdateGroupStatus(@Param("ids") List<Long> ids, @Param("groupStatus") Integer groupStatus);

    /**
     * 统计进行中的用户发起拼团数量（group_status=0, delete_flag=0）
     *
     * @return 数量
     */
    int countActiveInitiatedGroups();

    /**
     * 原子递增用户发起拼团的 current_num（CAS 模式，防超卖）
     * 
     * @param id 发起记录ID
     * @return 影响行数（0 表示已满员）
     */
    int incrementCurrentNumAtomic(@Param("id") Long id);

    /**
     * 批量更新用户发起拼团下所有参团记录的状态和服务器时间
     * 
     * @param initiatedId 发起记录ID（作为 osh_group_work.group_activity_id）
     * @param groupStatus 目标状态
     * @param serverStartTime 服务器开始时间
     * @param serverExpireTime 服务器到期时间
     * @return 影响行数
     */
    int batchUpdateWorkStatusByInitiatedId(
            @Param("initiatedId") Long initiatedId,
            @Param("groupStatus") Integer groupStatus,
            @Param("serverStartTime") LocalDateTime serverStartTime,
            @Param("serverExpireTime") LocalDateTime serverExpireTime);
}
