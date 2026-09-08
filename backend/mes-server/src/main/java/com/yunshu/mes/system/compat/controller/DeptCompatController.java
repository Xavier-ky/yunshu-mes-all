package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.DeptCompatRepository;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/system/dept")
public class DeptCompatController {

    private final DeptCompatRepository deptRepo;

    public DeptCompatController(DeptCompatRepository deptRepo) {
        this.deptRepo = deptRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.ok(deptRepo.selectList(params));
    }

    @GetMapping("/list/exclude/{deptId}")
    public Map<String, Object> excludeChild(@PathVariable Long deptId) {
        List<Map<String, Object>> depts = new ArrayList<>(deptRepo.selectList(Map.of()));
        Iterator<Map<String, Object>> it = depts.iterator();
        while (it.hasNext()) {
            Map<String, Object> d = it.next();
            Long id = SysCompatHelper.longVal(d.get("deptId"));
            String ancestors = SysCompatHelper.str(d.get("ancestors"));
            if (id.equals(deptId) || (ancestors != null && ancestors.contains("," + deptId))) {
                it.remove();
            }
        }
        return MesApiResponse.ok(depts);
    }

    @GetMapping("/{deptId}")
    public Map<String, Object> getInfo(@PathVariable Long deptId) {
        return deptRepo.findById(deptId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("部门不存在"));
    }

    @GetMapping("/treeselect")
    public Map<String, Object> treeselect(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> flat = deptRepo.selectList(params);
        return MesApiResponse.ok(SysCompatHelper.buildTreeSelect(flat, "deptId", "parentId", "deptName"));
    }

    @GetMapping("/roleDeptTreeselect/{roleId}")
    public Map<String, Object> roleDeptTreeselect(@PathVariable Long roleId) {
        List<Map<String, Object>> flat = deptRepo.selectList(Map.of());
        Map<String, Object> resp = MesApiResponse.ok();
        resp.put("checkedKeys", deptRepo.selectDeptIdsByRoleId(roleId));
        resp.put("depts", SysCompatHelper.buildTreeSelect(flat, "deptId", "parentId", "deptName"));
        return resp;
    }

    @OperLog(title = "部门管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String deptName = SysCompatHelper.str(body.get("deptName"));
        Long parentId = SysCompatHelper.longObj(body.get("parentId"));
        if (deptRepo.deptNameExists(deptName, parentId, null)) {
            return MesApiResponse.error("新增部门'" + deptName + "'失败，部门名称已存在");
        }
        return MesApiResponse.ok(deptRepo.insert(body));
    }

    @OperLog(title = "部门管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long deptId = SysCompatHelper.longObj(body.get("deptId"));
        String deptName = SysCompatHelper.str(body.get("deptName"));
        Long parentId = SysCompatHelper.longObj(body.get("parentId"));
        if (deptId != null && deptId.equals(parentId)) {
            return MesApiResponse.error("修改部门'" + deptName + "'失败，上级部门不能是自己");
        }
        if (deptRepo.deptNameExists(deptName, parentId, deptId)) {
            return MesApiResponse.error("修改部门'" + deptName + "'失败，部门名称已存在");
        }
        return MesApiResponse.toAjax(deptRepo.update(body));
    }

    @OperLog(title = "部门管理", businessType = 3)
    @DeleteMapping("/{deptId}")
    public Map<String, Object> remove(@PathVariable Long deptId) {
        if (deptRepo.hasChildren(deptId)) {
            return MesApiResponse.error("存在下级部门,不允许删除");
        }
        if (deptRepo.hasUsers(deptId)) {
            return MesApiResponse.error("部门存在用户,不允许删除");
        }
        return MesApiResponse.toAjax(deptRepo.deleteById(deptId));
    }
}
