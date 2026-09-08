package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.MenuCompatRepository;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/system/menu")
public class MenuCompatController {

    private final MenuCompatRepository menuRepo;

    public MenuCompatController(MenuCompatRepository menuRepo) {
        this.menuRepo = menuRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.ok(menuRepo.selectList(params));
    }

    @GetMapping("/{menuId}")
    public Map<String, Object> getInfo(@PathVariable Long menuId) {
        return menuRepo.findById(menuId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("菜单不存在"));
    }

    @GetMapping("/treeselect")
    public Map<String, Object> treeselect(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> flat = menuRepo.selectList(params);
        return MesApiResponse.ok(SysCompatHelper.buildTreeSelect(flat, "menuId", "parentId", "menuName"));
    }

    @GetMapping("/roleMenuTreeselect/{roleId}")
    public Map<String, Object> roleMenuTreeselect(@PathVariable Long roleId) {
        List<Map<String, Object>> flat = menuRepo.selectList(Map.of());
        Map<String, Object> resp = MesApiResponse.ok();
        resp.put("checkedKeys", menuRepo.selectMenuIdsByRoleId(roleId));
        resp.put("menus", SysCompatHelper.buildTreeSelect(flat, "menuId", "parentId", "menuName"));
        return resp;
    }

    @OperLog(title = "菜单管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String menuName = SysCompatHelper.str(body.get("menuName"));
        Long parentId = SysCompatHelper.longObj(body.get("parentId"));
        if (menuRepo.menuNameExists(menuName, parentId, null)) {
            return MesApiResponse.error("新增菜单'" + menuName + "'失败，菜单名称已存在");
        }
        return MesApiResponse.ok(menuRepo.insert(body));
    }

    @OperLog(title = "菜单管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long menuId = SysCompatHelper.longObj(body.get("menuId"));
        Long parentId = SysCompatHelper.longObj(body.get("parentId"));
        String menuName = SysCompatHelper.str(body.get("menuName"));
        if (menuId != null && menuId.equals(parentId)) {
            return MesApiResponse.error("修改菜单'" + menuName + "'失败，上级菜单不能选择自己");
        }
        if (menuRepo.menuNameExists(menuName, parentId, menuId)) {
            return MesApiResponse.error("修改菜单'" + menuName + "'失败，菜单名称已存在");
        }
        return MesApiResponse.toAjax(menuRepo.update(body));
    }

    @OperLog(title = "菜单管理", businessType = 3)
    @DeleteMapping("/{menuId}")
    public Map<String, Object> remove(@PathVariable Long menuId) {
        if (menuRepo.hasChildren(menuId)) {
            return MesApiResponse.error("存在子菜单,不允许删除");
        }
        return MesApiResponse.toAjax(menuRepo.deleteById(menuId));
    }
}
