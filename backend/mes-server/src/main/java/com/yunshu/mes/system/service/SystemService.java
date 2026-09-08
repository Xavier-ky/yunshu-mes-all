package com.yunshu.mes.system.service;

import com.yunshu.mes.system.dto.AuthUser;
import com.yunshu.mes.system.dto.UserCreateDTO;
import com.yunshu.mes.system.vo.RoleVO;
import com.yunshu.mes.system.vo.UserVO;
import java.util.List;
import java.util.Optional;

/**
 * 系统管理域数据服务，统一封装用户、角色查询与登录凭证查询。
 * 实现可基于 JDBC（连库时）或 Mock（无库时回退），调用方只依赖此接口。
 */
public interface SystemService {

    List<UserVO> listUsers(String keyword, String status);

    Optional<UserVO> getUserById(Long id);

    UserVO createUser(UserCreateDTO dto);

    UserVO updateUser(Long id, UserCreateDTO dto);

    void deleteUser(Long id);

    List<RoleVO> listRoles();

    Optional<AuthUser> findAuthUser(String username);
}
