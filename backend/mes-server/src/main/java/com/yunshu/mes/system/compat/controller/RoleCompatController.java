package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.MenuCompatRepository;
import com.yunshu.mes.system.compat.repository.RoleCompatRepository;
import com.yunshu.mes.system.compat.repository.UserCompatRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/role")
public class RoleCompatController {

    private final RoleCompatRepository roleRepo;
    private final UserCompatRepository userRepo;
    private final MenuCompatRepository menuRepo;

    public RoleCompatController(RoleCompatRepository roleRepo, UserCompatRepository userRepo,
                                MenuCompatRepository menuRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.menuRepo = menuRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = roleRepo.search(params, SysCompatHelper.offset(pn, ps), ps);
        return MesApiResponse.table(rows, roleRepo.count(params));
    }

    @GetMapping("/{roleId}")
    public Map<String, Object> getInfo(@PathVariable Long roleId) {
        return roleRepo.findById(roleId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("角色不存在"));
    }

    @OperLog(title = "角色管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String roleName = SysCompatHelper.str(body.get("roleName"));
        String roleKey = SysCompatHelper.strOr(body.get("roleKey"), SysCompatHelper.str(body.get("roleCode")));
        if (roleRepo.roleNameExists(roleName, null)) {
            return MesApiResponse.error("新增角色'" + roleName + "'失败，角色名称已存在");
        }
        if (StringUtils.hasText(roleKey) && roleRepo.roleKeyExists(roleKey, null)) {
            return MesApiResponse.error("新增角色'" + roleName + "'失败，角色权限已存在");
        }
        return MesApiResponse.ok(roleRepo.insert(body));
    }

    @OperLog(title = "角色管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long roleId = SysCompatHelper.longObj(body.get("roleId"));
        String roleName = SysCompatHelper.str(body.get("roleName"));
        String roleKey = SysCompatHelper.strOr(body.get("roleKey"), SysCompatHelper.str(body.get("roleCode")));
        if (roleRepo.roleNameExists(roleName, roleId)) {
            return MesApiResponse.error("修改角色'" + roleName + "'失败，角色名称已存在");
        }
        if (StringUtils.hasText(roleKey) && roleRepo.roleKeyExists(roleKey, roleId)) {
            return MesApiResponse.error("修改角色'" + roleName + "'失败，角色权限已存在");
        }
        return MesApiResponse.toAjax(roleRepo.update(body));
    }

    @OperLog(title = "角色管理", businessType = 2)
    @PutMapping("/dataScope")
    public Map<String, Object> dataScope(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(roleRepo.updateDataScope(body));
    }

    @OperLog(title = "角色管理", businessType = 2)
    @PutMapping("/changeStatus")
    public Map<String, Object> changeStatus(@RequestBody Map<String, Object> body) {
        Long roleId = SysCompatHelper.longObj(body.get("roleId"));
        if (roleId == null) {
            return MesApiResponse.error("角色ID不能为空");
        }
        return MesApiResponse.toAjax(roleRepo.changeStatus(roleId, SysCompatHelper.str(body.get("status"))));
    }

    @OperLog(title = "角色管理", businessType = 3)
    @DeleteMapping("/{roleIds}")
    public Map<String, Object> remove(@PathVariable String roleIds) {
        return MesApiResponse.toAjax(roleRepo.deleteByIds(SysCompatHelper.parseIds(roleIds)));
    }

    @GetMapping("/optionselect")
    public Map<String, Object> optionselect() {
        return MesApiResponse.ok(roleRepo.selectAll());
    }

    @GetMapping("/authUser/allocatedList")
    public Map<String, Object> allocatedList(@RequestParam Map<String, String> params) {
        Long roleId = SysCompatHelper.longObj(params.get("roleId"));
        if (roleId == null) {
            return MesApiResponse.table(List.of(), 0);
        }
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = userRepo.selectAllocatedUsers(params, roleId, SysCompatHelper.offset(pn, ps), ps);
        return MesApiResponse.table(rows, userRepo.countAllocatedUsers(params, roleId));
    }

    @GetMapping("/authUser/unallocatedList")
    public Map<String, Object> unallocatedList(@RequestParam Map<String, String> params) {
        Long roleId = SysCompatHelper.longObj(params.get("roleId"));
        if (roleId == null) {
            return MesApiResponse.table(List.of(), 0);
        }
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = userRepo.selectUnallocatedUsers(params, roleId, SysCompatHelper.offset(pn, ps), ps);
        return MesApiResponse.table(rows, userRepo.countUnallocatedUsers(params, roleId));
    }

    @OperLog(title = "角色管理", businessType = 4)
    @PutMapping("/authUser/cancel")
    public Map<String, Object> cancelAuthUser(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(roleRepo.deleteAuthUser(
                SysCompatHelper.longVal(body.get("roleId")),
                SysCompatHelper.longVal(body.get("userId"))));
    }

    @OperLog(title = "角色管理", businessType = 4)
    @PutMapping("/authUser/cancelAll")
    public Map<String, Object> cancelAuthUserAll(@RequestParam Long roleId, @RequestParam Long[] userIds) {
        return MesApiResponse.toAjax(roleRepo.deleteAuthUsers(roleId, userIds));
    }

    @OperLog(title = "角色管理", businessType = 4)
    @PutMapping("/authUser/selectAll")
    public Map<String, Object> selectAuthUserAll(@RequestParam Long roleId, @RequestParam Long[] userIds) {
        return MesApiResponse.toAjax(roleRepo.insertAuthUsers(roleId, userIds));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
