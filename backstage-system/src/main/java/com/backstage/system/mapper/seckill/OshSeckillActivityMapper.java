package com.backstage.system.mapper.seckill;

import com.backstage.system.domain.seckill.OshSeckillActivity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀活动 Mapper 接口
 *
 * @author backstage
 * @date 2026-04-28
 */
public interface OshSeckillActivityMapper {

    /** 根据ID查询 */
    OshSeckillActivity selectActivityById(Long id);

    /** 列表查询（支持按 title 模糊、status 过滤） */
    List<OshSeckillActivity> selectActivityList(OshSeckillActivity activity);

    /** 新增 */
    int insertActivity(OshSeckillActivity activity);

    /** 修改 */
    int updateActivity(OshSeckillActivity activity);

    /** 批量逻辑删除 */
    int deleteActivityByIds(@Param("ids") List<Long> ids);

    /** 批量更新活动状态 */
    int updateActivityStatusByIds(@Param("ids") List<Long> ids, @Param("status") Integer status);

    /**
     * 查询即将开始的活动（status=1 且 start_time <= now）
     */
    List<OshSeckillActivity> selectActivitiesToStart();

    /**
     * 查询即将结束的活动（status=2 且 end_time <= now）
     */
    List<OshSeckillActivity> selectActivitiesToEnd();

    /**
     * 将"未开始"且开始时间已到的活动改为"进行中"
     * status: 1→2，start_time <= now
     *
     * @param ids 本次查询命中的活动 ID 列表（select 与 update 范围对齐，
     *            防止新到期活动被更新但无对应缓存预热/ES 事件）
     */
    int updateToOngoing(@Param("ids") List<Long> ids);

    /**
     * 将"进行中"且结束时间已过的活动改为"已结束"
     * status: 2→3，end_time <= now
     *
     * @param ids 本次查询命中的活动 ID 列表（select 与 update 范围对齐）
     */
    int updateToFinished(@Param("ids") List<Long> ids);
}
