package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.PostCompatRepository;
import com.yunshu.mes.system.compat.repository.RoleCompatRepository;
import com.yunshu.mes.system.compat.repository.UserCompatRepository;
import com.yunshu.mes.system.compat.service.UserImportService;
import com.yunshu.mes.system.compat.service.UserImportService.ImportResult;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/system/user")
public class UserCompatController {

    private final UserCompatRepository userRepo;
    private final RoleCompatRepository roleRepo;
    private final PostCompatRepository postRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserImportService userImportService;

    public UserCompatController(UserCompatRepository userRepo, RoleCompatRepository roleRepo,
                                PostCompatRepository postRepo, PasswordEncoder passwordEncoder,
                                UserImportService userImportService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.postRepo = postRepo;
        this.passwordEncoder = passwordEncoder;
        this.userImportService = userImportService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = userRepo.search(params, SysCompatHelper.offset(pn, ps), ps);
        return MesApiResponse.table(rows, userRepo.count(params));
    }

    @GetMapping(value = { "/", "/{userId}" })
    public Map<String, Object> getInfo(@PathVariable(required = false) Long userId) {
        Map<String, Object> resp = MesApiResponse.ok();
        List<Map<String, Object>> roles = roleRepo.selectAll();
        resp.put("roles", roles);
        resp.put("posts", postRepo.selectAll());
        if (userId != null) {
            userRepo.findById(userId).ifPresent(u -> {
                resp.put("data", u);
                resp.put("postIds", userRepo.selectPostIds(userId));
                resp.put("roleIds", userRepo.selectRoleIds(userId));
            });
        }
        return resp;
    }

    @OperLog(title = "用户管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String userName = SysCompatHelper.str(body.get("userName"));
        if (!StringUtils.hasText(userName)) {
            return MesApiResponse.error("登录账号不能为空");
        }
        if (userRepo.usernameExists(userName, null)) {
            return MesApiResponse.error("新增用户'" + userName + "'失败，登录账号已存在");
        }
        String phone = SysCompatHelper.str(body.get("phonenumber"));
        if (StringUtils.hasText(phone) && userRepo.phoneExists(phone, null)) {
            return MesApiResponse.error("新增用户'" + userName + "'失败，手机号码已存在");
        }
        String email = SysCompatHelper.str(body.get("email"));
        if (StringUtils.hasText(email) && userRepo.emailExists(email, null)) {
            return MesApiResponse.error("新增用户'" + userName + "'失败，邮箱账号已存在");
        }
        String pwd = SysCompatHelper.strOr(body.get("password"), "123456");
        try {
            Long id = userRepo.insert(body, passwordEncoder.encode(pwd));
            return MesApiResponse.ok(id);
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.error(ex.getMessage());
        }
    }

    @OperLog(title = "用户管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long userId = SysCompatHelper.longObj(body.get("userId"));
        String phone = SysCompatHelper.str(body.get("phonenumber"));
        if (StringUtils.hasText(phone) && userRepo.phoneExists(phone, userId)) {
            return MesApiResponse.error("修改用户失败，手机号码已存在");
        }
        String email = SysCompatHelper.str(body.get("email"));
        if (StringUtils.hasText(email) && userRepo.emailExists(email, userId)) {
            return MesApiResponse.error("修改用户失败，邮箱账号已存在");
        }
        try {
            return MesApiResponse.toAjax(userRepo.update(body));
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.error(ex.getMessage());
        }
    }

    @OperLog(title = "用户管理", businessType = 3)
    @DeleteMapping("/{userIds}")
    public Map<String, Object> remove(@PathVariable String userIds) {
        List<Long> ids = SysCompatHelper.parseIds(userIds);
        return MesApiResponse.toAjax(userRepo.deleteByIds(ids));
    }

    @OperLog(title = "用户管理", businessType = 2)
    @PutMapping("/resetPwd")
    public Map<String, Object> resetPwd(@RequestBody Map<String, Object> body) {
        Long userId = SysCompatHelper.longObj(body.get("userId"));
        String pwd = SysCompatHelper.str(body.get("password"));
        if (userId == null || !StringUtils.hasText(pwd)) {
            return MesApiResponse.error("参数不完整");
        }
        return MesApiResponse.toAjax(userRepo.resetPwd(userId, passwordEncoder.encode(pwd)));
    }

    @OperLog(title = "用户管理", businessType = 2)
    @PutMapping("/changeStatus")
    public Map<String, Object> changeStatus(@RequestBody Map<String, Object> body) {
        Long userId = SysCompatHelper.longObj(body.get("userId"));
        if (userId == null) {
            return MesApiResponse.error("用户ID不能为空");
        }
        return MesApiResponse.toAjax(userRepo.changeStatus(userId, SysCompatHelper.str(body.get("status"))));
    }

    @GetMapping("/authRole/{userId}")
    public Map<String, Object> authRole(@PathVariable Long userId) {
        Map<String, Object> resp = MesApiResponse.ok();
        userRepo.findById(userId).ifPresent(u -> resp.put("user", u));
        resp.put("roles", userRepo.selectRolesByUserId(userId));
        return resp;
    }

    @OperLog(title = "用户管理", businessType = 4)
    @PutMapping("/authRole")
    public Map<String, Object> insertAuthRole(@RequestParam Long userId, @RequestParam(required = false) Long[] roleIds) {
        try {
            userRepo.saveUserAuth(userId, roleIds);
            return MesApiResponse.ok();
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.error(ex.getMessage());
        }
    }

    @OperLog(title = "用户管理", businessType = 5)
    @PostMapping("/export")
    public void export(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        writeXlsxAttachment(response, "user_export.xlsx");
        userImportService.writeExport(response.getOutputStream(), params);
    }

    @OperLog(title = "用户管理", businessType = 6)
    @PostMapping("/importData")
    public Map<String, Object> importData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", defaultValue = "0") int updateSupport) {
        try {
            ImportResult result = userImportService.importUsers(file, updateSupport == 1);
            Map<String, Object> resp = MesApiResponse.ok();
            resp.put("msg", result.htmlMsg());
            return resp;
        } catch (IOException ex) {
            return MesApiResponse.error("读取 Excel 失败：" + ex.getMessage());
        }
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException {
        writeXlsxAttachment(response, "user_import_template.xlsx");
        userImportService.writeTemplate(response.getOutputStream());
    }

    private static void writeXlsxAttachment(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }
}
