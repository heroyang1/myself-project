package com.backstage.system.mapper.user;

import com.backstage.system.domain.user.OshUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * OshUser: 九转苍翎
 * Date: 2026/3/7
 * Time: 16:44
 */
public interface OshUserMapper extends BaseMapper<OshUser> {
    OshUser getUserByUsername(String username);
//
//    OshUser getUserByUsernameOrEmail(String name);
//
//    OshUser getUserByEmail(String email);
//
//    int register(@Param("userId") Long userId, @Param("username") String username,@Param("password")  String password, @Param("email")  String email);
//
    int addUniqueId(@Param("userId") Long userId,@Param("uniqueId") String uniqueId);
//
    int addRole(@Param("userId") Long userId);
//
    String getUniqueIdByUserId(@Param("userId") Long userId);
//
    Long getUserIdByUniqueId(String uniqueId);
//
    int updateUniqueIdByUserId(@Param("userId") Long userId,@Param("uniqueId") String uniqueId);
//
//    int updatePasswordByEmail(@Param("email") String email, @Param("password") String password);
//
//    int updatePasswordById(@Param("userId") Long userId, @Param("password") String password);
//
//    int updateUserInfoById(@Param("userId") Long userId, @Param("avatar") String avatar, @Param("nickname") String nickname, @Param("sex") String sex);
//
//    String getPasswordById(@Param("userId") Long userId);
//
    OshUser getUserInfoById(@Param("userId") Long userId);
//
//    int updateEmailById(@Param("userId") Long userId, @Param("email") String email);

    int deleteUniqueId(Long userId);

//    //以下是若依代码生成
//
    /**
     * 查询用户
     *
     * @param id 用户主键
     * @return 用户
     */
    OshUser selectUserById(Long id);
//    /**
//     * 新增用户
//     *
//     * @param oshUser 用户
//     * @return 结果
//     */
//    public int insertUser(OshUser oshUser);
//
//    /**
//     * 修改用户
//     *
//     * @param oshUser 用户
//     * @return 结果
//     */
//    public int updateUser(OshUser oshUser);
//
//    /**
//     * 删除用户
//     *
//     * @param id 用户主键
//     * @return 结果
//     */
//    public int deleteUserById(Long id);
//
//    /**
//     * 批量删除用户
//     *
//     * @param ids 需要删除的数据主键集合
//     * @return 结果
//     */
//    public int deleteUserByIds(Long[] ids);
}
